package com.maxrave.simpmusic.expect

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.maxrave.logger.Logger
import com.maxrave.netease.NeteaseAntiCheatSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/*
 * 易盾 Watchman 反爬 token(社区称 checkToken v2)生成器:
 * headless WebView → baseURL 伪 music.163.com 源(壳页零网络) → WebView 网络栈自拉
 * acstatic-dun.126.net/tool.min.js → initWatchman(productNumber) → 按业务 key
 * w.getToken()。形状对照 kukume/neteasemusic-edgeone-makers _shared/lib/checkToken.ts
 * (常量同源);WebView 模式与 NeteaseFingerprint.android.kt 同宗。
 *
 * 常驻复用:WebView 一次创建进程生命周期内复用,重复生成只跑 JS(~几百 ms);
 * 任何失败返回 null(NeteaseAntiCheatSource 铁律:token 是增强项,绝不阻塞写路径)。
 */
private const val TAG = "WatchmanToken"
private const val MUSIC_ORIGIN = "https://music.163.com/"
private const val TOOL_SCRIPT_URL = "https://acstatic-dun.126.net/tool.min.js"
private const val WATCHMAN_PRODUCT_NUMBER = "YD00000558929251"
private const val SHELL_HTML =
    "<html><body></body><script src=\"$TOOL_SCRIPT_URL\"></script></html>"

class WatchmanTokenSource(
    context: Context,
) : NeteaseAntiCheatSource {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var webView: WebView? = null

    override suspend fun token(businessKey: String): String? =
        runCatching {
            withTimeoutOrNull(20_000) {
                mutex.withLock {
                    val wv = ensureWebView()
                    generate(wv, businessKey)
                }
            }
        }.getOrElse {
            Logger.w(TAG, "generate failed: ${it.message}")
            null
        }

    /** 壳页就绪+等易盾脚本注入 initWatchman;失败抛异常(由 token() 折叠为 null) */
    private suspend fun ensureWebView(): WebView =
        withContext(Dispatchers.Main) {
            webView?.let { return@withContext it }
            val pageLoaded = CompletableDeferred<Unit>()
            @SuppressLint("SetJavaScriptEnabled")
            val wv =
                WebView(appContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                pageLoaded.complete(Unit)
                            }
                        }
                    loadDataWithBaseURL(MUSIC_ORIGIN, SHELL_HTML, "text/html", "utf-8", null)
                }
            withTimeoutOrNull(10_000) { pageLoaded.await() } ?: run {
                runCatching { wv.destroy() }
                error("watchman shell page timeout")
            }
            // 外链脚本异步加载,轮询等 initWatchman/initNEWatchman 注入
            var waited = 0L
            while (waited < 15_000) {
                val has =
                    evaluate(wv, "(typeof initWatchman!=='undefined'||typeof initNEWatchman!=='undefined')") == "true"
                if (has) {
                    webView = wv
                    Logger.d(TAG, "webview ready in ${waited}ms")
                    return@withContext wv
                }
                delay(200)
                waited += 200
            }
            runCatching { wv.destroy() }
            error("initWatchman not available (tool.min.js load failed?)")
        }

    /** 触发 getToken 并轮询 window.__wmResult(对象由 evaluateJavascript 序列化为 JSON) */
    private suspend fun generate(
        wv: WebView,
        businessKey: String,
    ): String? {
        evaluate(
            wv,
            """
            (function(){
              try{
                var init = window.initWatchman || window.initNEWatchman;
                if(!init){ window.__wmResult = {ok:false, err:'no initWatchman'}; return; }
                window.__wmResult = null;
                init({productNumber:'$WATCHMAN_PRODUCT_NUMBER',
                  onload:function(w){
                    try{ w.getToken('$businessKey',
                      function(t){ window.__wmResult = {ok:true, token:(''+t)}; },
                      function(e){ window.__wmResult = {ok:false, err:(''+e)}; }); }
                    catch(e){ window.__wmResult = {ok:false, err:(''+e)}; }
                  },
                  onerror:function(e){ window.__wmResult = {ok:false, err:('init '+e)}; },
                  timeout:10000});
              }catch(e){ window.__wmResult = {ok:false, err:(''+e)}; }
            })();
            """.trimIndent(),
        )
        var waited = 0L
        while (waited < 15_000) {
            val raw = evaluate(wv, "window.__wmResult")
            if (raw != null && raw != "null") {
                // evaluateJavascript 把 JS 对象序列化成 JSON 字符串
                val ok = Regex("\"ok\"\\s*:\\s*true").containsMatchIn(raw)
                val token = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1)
                val err = Regex("\"err\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.get(1)
                if (ok && !token.isNullOrEmpty()) {
                    Logger.d(TAG, "token ok len=${token.length} biz=$businessKey")
                    return token
                }
                Logger.w(TAG, "token rejected ok=$ok err=$err biz=$businessKey")
                return null
            }
            delay(250)
            waited += 250
        }
        Logger.w(TAG, "token poll timeout biz=$businessKey")
        return null
    }

    private suspend fun evaluate(
        wv: WebView,
        script: String,
    ): String? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                wv.evaluateJavascript(script) { value -> cont.resume(value) }
            }
        }
}

/** App 启动时调用;装不上(Koin 竞态等)只打日志,绝不影响启动 */
fun installNeteaseWatchmanTokenSource(context: Context) {
    runCatching {
        org.koin.core.context.GlobalContext.get()
            .get<com.maxrave.data.repository.NeteaseRepositoryImpl>()
            .installAntiCheatSource(WatchmanTokenSource(context))
    }.onFailure { Logger.w(TAG, "install failed: ${it.message}") }
}
