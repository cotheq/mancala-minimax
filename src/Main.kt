package bantumi
import processing.core.PApplet
import java.util.*
import kotlin.collections.ArrayList

var game = Game()

// "HUMAN_HUMAN", "HUMAN_CPU", "CPU_CPU"
var GAMEMODE = "HUMAN_CPU"
var SCALE = 15
var DEPTH = 15

fun main(args: Array<String>) {
    if (args.size > 0)
        if (args[0].toInt() in 1..20) SCALE = args[0].toInt()
    if (args.size > 1)
        if (args[1] == "HUMAN_HUMAN" || args[1] == "HUMAN_CPU") GAMEMODE = args[1]
    if (args.size > 2)
        if (args[2].toInt() in 0..15) DEPTH = args[2].toInt()
    println("GAMEMODE: $GAMEMODE; SCALE: $SCALE; DEPTH: $DEPTH")
    PApplet.main("bantumi.Main")
}

class Main : PApplet()  {
    //color constants
    private val br = 179f; private val bg = 191f; private val bb = 53f
    private val fr = 21f; private val fg = 45f; private val fb = 26f

    //scale, cell width, cell height

    private val cw = 12
    private val ch = 20
    private var cellXY = HashMap<Int, Pair<Int, Int>>()

    //to save and visualize turns
    private var prevField = ArrayDeque<ArrayList<Int>>()
    private var lastQ = -1
    private var lastPF = ArrayList<Int>()
    private var lastChanged = -1
    private var lastMove = -1
    private var lastMoves = ArrayDeque<Int>()

    //flags to move and animate
    private var canMove = false
    private var moving = false
    private var animating = false
    private var animatingCell = false
    private var capture = false
    private var handle = false
    private var gameFinished = false

    private fun checkNumWidth(n: Int) = n.toString().length * 5 - 1

    private fun rectScale(a: Int, b: Int, c: Int, d: Int) {
        rect((a * SCALE).toFloat(), (b * SCALE).toFloat(), (c * SCALE).toFloat(), (d * SCALE).toFloat())
    }

    private fun drawDigit(x: Int, y: Int, d: Int) {
        if (d !in 0..9)
            return
        fun p(x: Int, y: Int) { rectScale(x, y, 1, 1) }
        for (i in 0..3) {
            for (j in 0..5) {
                val toDraw = when (Pair(i, j)) {
                    Pair(0, 0) -> { (d in listOf(0, 2, 3, 4, 5, 6, 7, 8, 9)) }
                    Pair(1, 0) -> { (d in listOf(0, 2, 3, 5, 6, 7, 8, 9)) }
                    Pair(2, 0) -> { (d in listOf(0, 1, 2, 3, 5, 6, 7, 8, 9)) }
                    Pair(3, 0) -> { (d in listOf(0, 2, 3, 4, 5, 6, 7, 8, 9)) }
                    Pair(0, 1) -> { (d in listOf(0, 4, 5, 6, 8, 9)) }
                    Pair(1, 1) -> { (d in listOf(1)) }
                    Pair(2, 1) -> { (d in listOf(1)) }
                    Pair(3, 1) -> { (d in listOf(0, 2, 3, 4, 7, 8, 9)) }
                    Pair(0, 2) -> { (d in listOf(0, 2, 4, 5, 6, 8, 9)) }
                    Pair(1, 2) -> { (d in listOf(2, 3, 4, 5, 6, 8, 9)) }
                    Pair(2, 2) -> { (d in listOf(1, 2, 3, 4, 5, 6, 8, 9)) }
                    Pair(3, 2) -> { (d in listOf(0, 2, 3, 4, 5, 6, 7, 8, 9)) }
                    Pair(0, 3) -> { (d in listOf(0, 2, 6, 8)) }
                    Pair(2, 3) -> { (d in listOf(1)) }
                    Pair(3, 3) -> { (d in listOf(0, 3, 4, 5, 6, 7, 8, 9)) }
                    Pair(0, 4) -> { (d in listOf(0, 2, 6, 8)) }
                    Pair(2, 4) -> { (d in listOf(1)) }
                    Pair(3, 4) -> { (d in listOf(0, 3, 4, 5, 6, 7, 8, 9)) }
                    Pair(0, 5) -> { (d in listOf(0, 2, 3, 5, 6, 8, 9)) }
                    Pair(1, 5) -> { (d in listOf(0, 1, 2, 3, 5, 6, 8, 9)) }
                    Pair(2, 5) -> { (d in listOf(0, 1, 2, 3, 5, 6, 8, 9)) }
                    Pair(3, 5) -> { (d in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
                    else -> false
                }
                if (toDraw) p(x + i, y + j)
            }
        }
    }

    private fun drawNumber(x: Int, y: Int, n: Int) {
        val s = n.toString()
        for (i in 0 until s.length) {
            drawDigit(x + i * 5, y, (s[i] + "").toInt())
        }
    }

    private fun drawChangedCell(i: Int, n: Int) {
        if (i == -1) return
        noStroke()
        fill(fr, fg, fb)
        val x = cellXY[i]!!.first
        val y = cellXY[i]!!.second
        when (i) {
            in 0..5, in 7..12 -> {
                rectScale(x, y, cw, cw)
                fill(br, bg, bb)
                drawNumber(x + cw / 2 - checkNumWidth(n) / 2, y + cw / 2 - 3, n)
            }
            in listOf(6, 13) -> {
                rectScale(x, y, cw, ch)
                fill(br, bg, bb)
                drawNumber(x + cw / 2 - checkNumWidth(n) / 2, y + ch / 2 - 3, n)
            }
        }
    }

    private fun drawField(f: ArrayList<Int>? = null) {
        background(br, bg, bb)
        stroke(fr, fg, fb)
        fill(br, bg, bb)
        strokeWeight((SCALE / 1.5).toFloat())
        for (i in 0..5) {
            rectScale(1 + 14 * i, 1, cw, cw)
            cellXY[i] = Pair(1 + 14 * i, 35)
            rectScale(1 + 14 * i, 35, cw, cw)
            cellXY[12 - i] = Pair(1 + 14 * i, 1)

        }

        rectScale(1, 14 , cw, ch)
        cellXY[6] = Pair(71, 14)
        rectScale(71, 14 , cw, ch)
        cellXY[13] = Pair(1, 14)

        var x: Int
        var y: Int
        var n: Int
        for (i in 0..5) {
            noStroke()
            fill(fr, fg, fb)
            x = cellXY[i]!!.first
            y = cellXY[i]!!.second
            n = if (f == null) game.bottom[i] else f[i]
            drawNumber(x + cw / 2 - checkNumWidth(n) / 2, y + cw / 2 - 3, n)

            x = cellXY[12 - i]!!.first
            y = cellXY[12 - i]!!.second
            n = if (f == null) game.top[5 - i] else f[12 - i]
            drawNumber(x + cw / 2 - checkNumWidth(n) / 2, y + cw / 2 - 3, n)
        }
        for (i in listOf(6, 13)) {
            x = cellXY[i]!!.first
            y = cellXY[i]!!.second
            n = when {
                f != null -> f[i]
                i == 6 -> game.bottom[6]
                i == 13 -> game.top[6]
                else -> -1
            }
            drawNumber(x + cw / 2 - checkNumWidth(n) / 2, y + ch / 2 - 3, n)
        }
    }

    private fun getCell(): Int {
        for (c in cellXY) {
            val x = c.value.first
            val y = c.value.second
            if (mouseX / SCALE in x .. x + cw && mouseY / SCALE in y .. y + cw) {
                return c.key
            }
        }
        return -1
    }

    private fun animateCell(c: Int, pf: ArrayList<Int>, m: Int) {
        animatingCell = true
        if (lastQ > 0) {
            val n = if (c == m) 0 else pf[c] + 1
            lastChanged = (c + 1) % 14
            if (m in 0..5 && lastChanged == 13) {
                lastChanged = 0
            }
            if (m in 7..12 && lastChanged == 6) {
                lastChanged++
            }

            drawChangedCell(c, n)
            lastQ--

        } else {
            lastChanged--
            if (lastChanged == -1)
                lastChanged = 13

            if ((m in 0..5 && lastChanged in 0..5) || (m in 7..12 && lastChanged in 7..12)) {
                if (lastPF[lastChanged] == 0) {
                    handle = (lastPF[12 - lastChanged] == 0)
                    capture = (lastPF[12 - lastChanged] > 0)
                    //println("WHEN CAPTURE: LASTPF: $lastPF; LAST CHANGED: $lastChanged")
                } else {
                    handle = false
                    capture = false
                }

            }



            if (prevField.size > 0)
                drawField(prevField.first)
            else {
                drawField()
            }

            if (!capture && !handle) {
                animatingCell = false
                return

            }
            if (capture) {
                capture = false
                handle = false
                drawChangedCell(lastChanged, 0)
                if (lastPF[12 - lastChanged] > 0) {
                    drawChangedCell(12 - lastChanged, 0)
                } else {
                    throw Exception("FUCK YOU ASSHOLE", Throwable("FuckYouException"))
                }
                var i: Int = -1
                if (m in 0..5)
                    i = 6
                else if (m in 7..12)
                    i = 13
                drawChangedCell(i, lastPF[i] + 1 + lastPF[12 - lastChanged])
                lastChanged = -1
                return
            }
            if (handle) {
                lastChanged = -1
                handle = false
                drawChangedCell(lastChanged, 0)
                var i: Int = -1
                if (m in 0..5)
                    i = 6
                else if (m in 7..12)
                    i = 13
                drawChangedCell(i, lastPF[i] + 1)
                return
            }
        }
    }

    private fun playHumanHuman() {
        if (!game.finished) {
            canMove = false
            if (game.turn) {
                val m = getCell()
                if (m in 0..5 && game.turn) {
                    if (game.bottom[m] > 0) {
                        prevField.add(ArrayList(game.bottom + game.top))
                        if (!game.move(m)) {
                            return
                        } else {
                            lastMoves.add(m)
                            animating = true
                            game.printField()
                        }
                    }
                }
            } else {
                val m = getCell()
                if (m in 7..12 && !game.turn) {
                    if (game.top[m - 7] > 0) {
                        prevField.add(ArrayList(game.bottom + game.top))
                        if (!game.move(m)) {
                            return
                        } else {
                            //println(m)
                            lastMoves.add(m)
                            //println(lastMoves)
                            animating = true
                            game.printField()
                        }
                    }
                }
            }

        }
    }

    private fun checkFinish() {
        var topSum = 0
        var bottomSum = 0
        for (i in 0..5) {
            topSum += game.top[i]
            bottomSum += game.bottom[i]
        }
        if (topSum == 0 || bottomSum == 0) {
            gameFinished = true
        }
    }

    private fun playHumanCPU() {
        if (!gameFinished) {
            checkFinish()
            canMove = false
            if (game.turn) {
                canMove = false
                val m = getCell()
                if (m in 0..5) {
                    if (game.bottom[m] > 0) {
                        prevField.add(ArrayList(game.bottom + game.top))
                        gameFinished = !game.move(m)
                        lastMoves.add(m)
                        animating = true
                        game.printField()
                    }
                }
            }

            while (!game.turn) {
                moving = true
                prevField.add(ArrayList(game.bottom + game.top))
                val m = game.getBestNextMove(DEPTH)
                gameFinished = !game.move(m)
                lastMoves.add(m)
                animating = true
                game.printField()
                checkFinish()
                if (gameFinished) break
            }
            canMove = false
        }
    }

    /*
    private fun playCpuCPU() {
        if (!gameFinished) {

            while (game.turn) {
                moving = true
                prevField.add(ArrayList(game.bottom + game.top))
                val m = game.getBestNextMove(DEPTH)
                gameFinished = !game.move(m)
                game.printField()
                checkFinish()
                if (gameFinished) break
                lastMoves.add(m)
                animating = true
            }

            while (!game.turn) {
                moving = true
                prevField.add(ArrayList(game.bottom + game.top))
                val m = game.getBestNextMove(DEPTH)
                gameFinished = !game.move(m)
                game.printField()
                checkFinish()
                if (gameFinished) break
                lastMoves.add(m)
                animating = true

                //println("gameFinished: $gameFinished")
            }
        } else {
            canMove = false
        }
    }
    */


    fun gameHandler() {
        if (canMove) {
            moving = true

            when (GAMEMODE) {
                "HUMAN_HUMAN" -> { playHumanHuman() }
                "HUMAN_CPU" -> { playHumanCPU() }
                //"CPU_CPU" -> { playCpuCPU()}
            }
            moving = false
        }
    }

    override fun settings() {
        size(84 * SCALE, 48 * SCALE)
        noSmooth()
    }

    override fun setup() {
        frameRate(5f)
        drawField()
    }

    override fun draw() {
        if (gameFinished) {
            when (frameCount % 6){
                0 -> { drawField() }
                3 -> {
                    drawChangedCell(6, game.bottom[6])
                    drawChangedCell(13, game.top[6])
                }
            }
            return
        }
            if (!animating) {
                clear()
                background(br, bg, bb)
                drawField()
                if (!gameFinished) {
                    thread("gameHandler")
                }
            } else {
                if (frameCount % 3 == 0) {
                    if (!capture && !handle) {
                        if (!animatingCell) {
                            val m: Int
                            if (lastMoves.size > 0) {
                                lastMove = lastMoves.first()

                                m = lastMove
                                lastMoves.pop()
                                lastPF = prevField.pop()
                                lastQ = lastPF[m] + 1
                                //println("LAST MOVES: $lastMoves")
                                //println("LAST PF: $lastPF")
                                animateCell(m, lastPF, m)
                            } else {
                                animatingCell = false
                                animating = false
                            }

                        } else {
                            animateCell(lastChanged, lastPF, lastMove)
                        }
                    }
                }
            }
    }

    override fun mouseReleased() {
        if (!gameFinished) {
            if (GAMEMODE != "CPU_CPU" && !canMove) {
                val m = getCell()
                if ((m in 0..5 || m in 7..12) && !moving && !animating)
                    canMove = true
            } else {
                canMove = true
            }
        }
    }

}