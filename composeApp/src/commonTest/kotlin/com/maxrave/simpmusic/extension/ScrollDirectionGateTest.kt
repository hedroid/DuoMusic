package com.maxrave.simpmusic.extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ScrollDirectionGate 的方向翻转迟滞(CR 2026-10-28:用户抓的 item 边界反例)。
 * 首版拿"index 变化=立即过阈"当大位移启发式,手指在两项交界处 ±3px 来回蹭会让
 * firstVisibleItemIndex 逐次跨界、每次跨界都接受翻转——迟滞门在边界完全失效。
 * 修正后净位移量=锚点项自己的视口内 offset 差,本测试锁定该语义。
 */
class ScrollDirectionGateTest {
    /** 项号→当前视口内主轴 offset;不在视口=缺 key。模拟 visibleItemsInfo 查询。 */
    private class FakeViewport(vararg pairs: Pair<Int, Int>) {
        private val map = pairs.toMap().toMutableMap()
        fun lookup(index: Int): Int? = map[index]
        fun moveTo(vararg pairs: Pair<Int, Int>) {
            map.clear()
            map.putAll(pairs)
        }
    }

    @Test
    fun 边界抖动不翻转_锚点项仍在视口() {
        val gate = ScrollDirectionGate(thresholdPx = 52)
        // 已接受上滚,锚点=项10(viewport offset 500)
        val vp = FakeViewport(10 to 500, 11 to 700, 12 to 900)
        assertTrue(gate.report(10, vp::lookup, rawUp = true))
        // 首个可见项在 10/11 交界来回跨界,项10 的视口 offset 只在 497~503 抖
        val jitters =
            listOf(
                11 to false, // 跨到 11=下滚翻转尝试(首版在此误接受)
                10 to true, // 跨回 10=上滚翻转尝试(首版在此又误接受)
                11 to false,
                10 to true,
                11 to false,
            )
        var anchorOffset = 500
        jitters.forEachIndexed { i, (firstVisible, rawUp) ->
            anchorOffset = 500 + if (i % 2 == 0) 3 else -3
            vp.moveTo(10 to anchorOffset, 11 to anchorOffset + 200, 12 to anchorOffset + 400)
            assertEquals(
                true,
                gate.report(firstVisible, vp::lookup, rawUp),
                "第 ${i + 1} 次边界抖动不应翻转方向",
            )
        }
    }

    @Test
    fun 同向上报全放行() {
        val gate = ScrollDirectionGate(thresholdPx = 52)
        val vp = FakeViewport(10 to 500, 11 to 700)
        assertTrue(gate.report(10, vp::lookup, rawUp = true))
        // 连续上滚(项10 的 offset 递增=朝顶),方向不变全放行
        var offset = 500
        repeat(5) {
            offset += 30
            vp.moveTo(10 to offset, 11 to offset + 200)
            assertTrue(gate.report(10, vp::lookup, rawUp = true))
        }
    }

    @Test
    fun 真实反向滚动过阈即翻转() {
        val gate = ScrollDirectionGate(thresholdPx = 52)
        val vp = FakeViewport(10 to 500, 11 to 700)
        assertTrue(gate.report(10, vp::lookup, rawUp = true))
        // 真下滚:锚点项10 的视口 offset 500→180,净位移 320>52
        vp.moveTo(10 to 180, 11 to 380)
        assertFalse(gate.report(10, vp::lookup, rawUp = false), "真实反向大位移应接受翻转")
        // 翻转后锚点重设在项10@180,继续同向下滚放行
        vp.moveTo(10 to 120, 11 to 320)
        assertFalse(gate.report(10, vp::lookup, rawUp = false))
    }

    @Test
    fun 锚点不可查不推定位移_维持方向后由真实位移决定() {
        val gate = ScrollDirectionGate(thresholdPx = 52)
        val vp = FakeViewport(10 to 500, 11 to 700)
        assertTrue(gate.report(10, vp::lookup, rawUp = true))
        // 锚点项 10 查不到(首个可见项贴边 1px 被推出视口/数据变更 index 失效):
        // 不推定大位移,维持上滚方向,原地重锚到当前首可见项 14@300
        vp.moveTo(14 to 300, 15 to 500)
        assertTrue(
            gate.report(14, vp::lookup, rawUp = false),
            "锚点不可查=无位移信息,不应直接接受翻转(二轮 CR:首可见项贴边 1px 即离开视口)",
        )
        // 随后真实下滚从新锚点量净位移:14@300→14@200(100>52)过阈翻转
        vp.moveTo(14 to 200, 15 to 400)
        assertFalse(gate.report(14, vp::lookup, rawUp = false), "重锚后的真实反向位移应照常过阈")
    }

    @Test
    fun 小反向净位移累计过阈才翻转_锚点不随抑制事件漂移() {
        val gate = ScrollDirectionGate(thresholdPx = 52)
        val vp = FakeViewport(10 to 500, 11 to 700)
        assertTrue(gate.report(10, vp::lookup, rawUp = true))
        // 反向 20px:不过阈,维持上滚
        vp.moveTo(10 to 480, 11 to 680)
        assertTrue(gate.report(10, vp::lookup, rawUp = false), "20px<52px 不应翻转")
        // 继续反向到累计 60px:过阈,翻转
        vp.moveTo(10 to 440, 11 to 640)
        assertFalse(gate.report(10, vp::lookup, rawUp = false), "累计 60px 过阈应翻转")
    }
}
