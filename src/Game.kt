package bantumi

fun main(args: Array<String>) {
    while (!game.finished) {
//        when (game.turn) {
//            true -> game.move(readLine()!!.toInt())
//            false -> game.move(game.getBestNextMove(DEPTH))
//        }
        game.move(game.getBestNextMove(DEPTH))
        game.printField()
    }
}

class Game {
    var top = ArrayList<Int>()
    var bottom = ArrayList<Int>()
    var turn = true
    var finished = false

    //new game
    constructor() {
        bottom = ArrayList(listOf(4, 4, 4, 4, 4, 4, 0))
        top = ArrayList(listOf(4, 4, 4, 4, 4, 4, 0))
        turn = true
        printField()
    }

    //continue game
    constructor(top: ArrayList<Int>, bottom: ArrayList<Int>) {
        this.bottom = top.clone() as ArrayList<Int>
        this.top = bottom.clone() as ArrayList<Int>
        turn = true
    }

    fun getBestNextMove(searchDepth: Int): Int {
        var bestValue = -1000.0
        var newValue = 0.0
        var bestMove = -1
        for (i in 5 downTo 0) {
            val test: Game

            if (turn) {
                if (bottom[i] == 0)
                    continue
                test = Game.newGameWithMove(bottom, top, i)
            } else {
                if (top[i] == 0)
                    continue
                test = Game.newGameWithMove(top, bottom, i)
            }

            // test.printField()
            if (test.turn) {
                newValue = Game.findBestNextMove(test.bottom, test.top, 100.0, bestValue, searchDepth)
            } else {
                newValue = -Game.findBestNextMove(test.top, test.bottom, -bestValue, -100.0, searchDepth)
            }

            //println("NEW xxxMOVE $i $newValue")
            if (newValue > bestValue) {
                bestMove = i
                bestValue = newValue
            }

        }
        println("best move $bestMove $bestValue")

        return if (turn) {
            bestMove
        } else {
            bestMove + 7
        }
    }

    fun printField() {
        println("  ${top[5]} ${top[4]} ${top[3]} ${top[2]} ${top[1]} ${top[0]}")
        println("${top[6]}              ${bottom[6]}")
        println("  ${bottom[0]} ${bottom[1]} ${bottom[2]} ${bottom[3]} ${bottom[4]} ${bottom[5]} ")
        println("Turn: " + when(turn) { true -> "BOTTOM" false -> "TOP"})
    }

    fun move(pos: Int): Boolean { // returns false if game is over
        //System.out.println("moving pos " + pos);
        // printField();
        val needFinishedCheck: Boolean
        if (turn) {
            if (pos > 5 || pos < 0) {
                /*println("tried to move pos " + pos
                                + " on _yer's turn")*/
                return true
            }
            needFinishedCheck = doMove(pos, bottom, top)
        } else {
            if (pos > 12 || pos < 7) {
                /*println("tried to move pos " + pos
                                + " on bottom player's turn")*/
                return true
            }
            needFinishedCheck = doMove(pos - 7, top, bottom)
        }

        if (needFinishedCheck) {
            var topCount = 0
            var bottomCount = 0
            for (i in 0..5) {
                topCount += top[i]
                bottomCount += bottom[i]
            }
            if (bottomCount == 0 || topCount == 0) {
                finishGame(topCount, bottomCount)
                return false
            }

        }
        // printField();
        return true
    }

    fun finishGame(topBonus: Int, bottomBonus: Int) {
        //printField();
        top[6] += topBonus
        bottom[6] += bottomBonus
        //System.out.println("Top score " + top[6])
        //System.out.println("Bottom score " + bottom[6])
        for (i in 0..5) {
            top[i] = 0
            bottom[i] = 0
        }
        finished = true
    }

    // implements pos, determines if we need to check if the game is over
    fun doMove(pos: Int, startBoard: ArrayList<Int>, otherBoard: ArrayList<Int>): Boolean {
        var possiblyOver = false
        val q = startBoard[pos]
        if (q == 0) {
            println("tried to pos an empty spot")
            return false
        }
        startBoard[pos] = 0
        var curPos = pos + 1
        for (i in q downTo 1) {
            when (curPos) {
                in 0..6 -> { // ie still on starting board
                    startBoard[curPos]++
                    curPos++
                }
                in 7..12 -> { // ie on top board
                    otherBoard[curPos - 7]++
                    curPos++
                }
                else -> { // have looped around
                    startBoard[0] += 1
                    curPos = 1
                }
            }
        }

        curPos-- // undo last increment since it ran out of stones

        //capture
        if (curPos < 6 && startBoard[curPos] == 1) {
            startBoard[6] = (startBoard[6] + otherBoard[5 - curPos] + 1)
            startBoard[curPos] = 0
            otherBoard[5 - curPos] = 0
            possiblyOver = true
        }

        if (curPos != 6)
            turn = !turn

        return possiblyOver || pos == 5
    }

    companion object {
        fun newGameWithMove(top: ArrayList<Int>, bottom: ArrayList<Int>, pos: Int): Game {
            val g = Game(top, bottom)
            g.move(pos)
            return g
        }

        private fun estimate(my: ArrayList<Int>, other: ArrayList<Int>): Double {
            var mySum = 0
            var otherSum = 0
            for (i in 0..5) {
                mySum += my[i]
                otherSum += other[i]
            }
            //return ((otherSum - mySum) * 0.5 + (other[6] - my[6]) * 1.5) //MAKE COMPUTER TO LOSE
            return ((mySum - otherSum) * 0.5 + (my[6] - other[6]) * 1.5)
        }

        // best guaranteed means that the player can do no better than that
        fun findBestNextMove(my: ArrayList<Int>, other: ArrayList<Int>, bestGuaranteed: Double, worstGuaranteed: Double, depth: Int): Double {
            // stop recursion somewhere
            if (depth <= 0 || my[6] + other[6] == 48) {
                return estimate(my, other)
            }

            var worst = worstGuaranteed
            var currentValue = -99999999.0
            for (i in 5 downTo 0) {
                if (my[i] != 0) { // ie valid move
                    val nextState = newGameWithMove(my, other, i)
                    val boardValue: Double

                    boardValue = when (nextState.turn) {
                        true -> findBestNextMove(nextState.bottom, nextState.top, bestGuaranteed, worst, depth - 1)
                        false -> -findBestNextMove(nextState.top, nextState.bottom, -worst, -bestGuaranteed, depth - 1)
                    }

                    // System.out.println("board value return " + boardValue);

                    if (bestGuaranteed <= boardValue) {
                        return boardValue
                    }
                    if (boardValue > worst) {
                        worst = boardValue
                    }

                    if (boardValue > currentValue) {
                        // System.out.println("best move so far " + boardValue);
                        // nextBoard.printField();
                        currentValue = boardValue
                    }
                }
            }
            return currentValue
        }
    }
}
