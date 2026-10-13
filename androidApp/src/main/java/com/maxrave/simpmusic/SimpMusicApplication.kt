package com.maxrave.simpmusic

import android.annotation.SuppressLint
import android.app.Application
import android.database.CursorWindow
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.expect.CloudUploadStore
import com.maxrave.simpmusic.expect.HapticFeedback
import com.maxrave.simpmusic.expect.installNeteaseWatchmanTokenSource
import com.maxrave.simpmusic.service.backup.AutoBackupScheduler
import com.maxrave.simpmusic.utils.NetworkFirstInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.AppContext
import okhttp3.OkHttpClient
import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.simpmusic.crashlytics.configCrashlytics
import org.simpmusic.lastfm.configLastfm
import java.io.File
import java.lang.reflect.Field

class SimpMusicApplication :
    Application(),
    KoinComponent,
    SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStoreManager: DataStoreManager by inject()
    private lateinit var autoBackupScheduler: AutoBackupScheduler

    override fun onCreate() {
        super.onCreate()
        // First, so the rest of startup reaches the App log too (Settings → Developer option).
        Logger.enableFileLogging(File(filesDir, "logs").path)
        configCrashlytics(this, BuildKonfig.sentryDsn)
        configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@SimpMusicApplication)
            loadAllModules(
                AppIdentity(
                    applicationId = BuildConfig.APPLICATION_ID,
                    versionName = BuildConfig.VERSION_NAME,
                    platform = "Android ${Build.VERSION.RELEASE}",
                    isDevBuild = BuildConfig.DEBUG,
                ),
            )
            loadKoinModules(viewModelModule)
        }
        // app log 暂停态恢复(DataStore 持久,2026-10-13 用户反馈"暂停后重启 app 又是打开
        // 状态"):Logger.fileLoggingEnabled 是进程内 @Volatile,重启即归位 true——启动读一次
        // 存储,暂停过就重新关掉文件记录(异步读,别阻塞主线程——DataStore actor 卡死前科)
        applicationScope.launch {
            Logger.fileLoggingEnabled =
                dataStoreManager.appLogFileLoggingPaused.first() != DataStoreManager.Values.TRUE
        }
        // 网易 eapi 反作弊 token(易盾 Watchman)生成器:headless WebView 懒初始化,
        // 生成失败自动降级纯 eapi——装不上也绝不能影响启动
        installNeteaseWatchmanTokenSource(this)
        // provide custom configuration
        val workConfig =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build()

        // initialize WorkManager
        WorkManager.initialize(this, workConfig)

        // Initialize and start AutoBackupScheduler
        autoBackupScheduler = AutoBackupScheduler(this, dataStoreManager)
        applicationScope.launch {
            autoBackupScheduler.observeAndSchedule()
        }

        // 触感反馈:订阅设置档位(播放控件振动),系统层未初始化前调用安全空跑
        HapticFeedback.initialize(this, dataStoreManager, applicationScope)

        // 云盘上传的 SAF URI 读取(非 Composable 上下文用,读文件/md5/开上传流)
        CloudUploadStore.appContext = this

        CaocConfig.Builder
            .create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) // default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) // default: true
            .showErrorDetails(true) // default: true
            .showRestartButton(true) // default: true
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false) // default: true
            .trackActivities(true) // default: false
            .minTimeBetweenCrashesMs(2000) // default: 3000 //default: bug image
            .restartActivity(MainActivity::class.java) // default: null (your app's launch activity)
            .apply()

        @SuppressLint("DiscouragedPrivateApi")
        val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
        field.isAccessible = true
        val expectSize = 100 * 1024 * 1024
        field.set(null, expectSize)

        AppContext.apply {
            set(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        Logger.w("Terminate", "Checking")
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        lateinit var imageLoader: ImageLoader
        imageLoader =
            ImageLoader
                .Builder(context)
                .components {
                    add(NetworkFirstInterceptor { imageLoader })
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                OkHttpClient()
                            },
                        ),
                    )
                }.diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .diskCache(
                    DiskCache
                        .Builder()
                        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                        .maxSizeBytes(512L * 1024 * 1024)
                        .build(),
                ).crossfade(true)
                .build()
        return imageLoader
    }
}