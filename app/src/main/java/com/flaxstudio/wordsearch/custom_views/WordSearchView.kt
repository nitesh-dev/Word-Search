package com.flaxstudio.wordsearch.custom_views

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.flaxstudio.wordsearch.R
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class WordSearchView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var gridSize = 0f
    private var halfGridSize = 0f
    private var currentGridLength: IntVector2 = IntVector2()
    private var parentOffset = FloatVector2()
    private lateinit var gridData: GridData
    var lineRadius = 0f

    private var viewSize = IntVector2()

    private val lineColors = arrayListOf(
        R.color.word_search_line_color_1,
        R.color.word_search_line_color_2,
        R.color.word_search_line_color_3,
        R.color.word_search_line_color_4,
        R.color.word_search_line_color_5,
        R.color.word_search_line_color_6,
        R.color.word_search_line_color_7,
        R.color.word_search_line_color_8,
        R.color.word_search_line_color_9,
        R.color.word_search_line_color_10
    )

    private var linePaint: Paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 45f

    }

    private var textPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.word_search_text_color)
        isFakeBoldText = true
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.jellee_bold_700)

    }

    private val outlinePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
    }


    private lateinit var tempWordNamesArray: Array<String>
    private var callWordSearchFunctionOnMeasure = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewSize.x = MeasureSpec.getSize(widthMeasureSpec)
        viewSize.y = MeasureSpec.getSize(heightMeasureSpec)

        gridSize = viewSize.x.toFloat() / currentGridLength.x

//        startWordSearch(tempWordNamesArray, 5, 5)
        setMeasuredDimension(viewSize.x, (gridSize * currentGridLength.y).toInt())

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (callWordSearchFunctionOnMeasure) {
            callWordSearchFunctionOnMeasure = false

            startWordSearch(tempWordNamesArray, currentGridLength.x, currentGridLength.y)

        }
    }


    private fun getNextLineColor(): Int {
        var index = drawnLineDataArray.size + hintsArray.size
        while (index >= lineColors.size) {
            index -= 10
        }

        return lineColors[index]
    }



    private val hintedWordsArray = ArrayList<WordData>()

    private val hintsArray = ArrayList<Hint>()
    private val removeHintsArray = ArrayList<Hint>()
    private val notSearchedAndHintedWordsArray = ArrayList<WordData>()

    private var hintsAlpha = 0
    private fun removeHints(thisView: View){

        ValueAnimator.ofInt(255, 0).apply {
                duration = 200
                interpolator = LinearInterpolator()
                addUpdateListener { valueAnimator ->
                    hintsAlpha = valueAnimator.animatedValue as Int
                    thisView.invalidate()
                }
            doOnEnd {
                removeHintsArray.clear()
                thisView.postInvalidate()
            }

        }.start()
    }

    fun isHintPossible(): Boolean{

        var isHinted: Boolean

        // finding word that is not searched and hinted
        notSearchedAndHintedWordsArray.clear()

        for (word in words) {

            if (!word.isSearched) {

                isHinted = false

                // check for not hinted
                for (hintWord in hintedWordsArray) {
                    if (word == hintWord) {
                        isHinted = true
                        break
                    }
                }

                if(!isHinted){
                    notSearchedAndHintedWordsArray.add(word)
                }

            }
        }

        if(notSearchedAndHintedWordsArray.size == 0){
            return false
        }

        // hint possible
        return true
    }

    fun showHint() {

        val randomIndex = (0 until notSearchedAndHintedWordsArray.size).shuffled().last()

        hintedWordsArray.add(notSearchedAndHintedWordsArray[randomIndex])

        val hint = Hint(gridToPos(notSearchedAndHintedWordsArray[randomIndex].startGrid), 0)
        hintsArray.add(hint)

        hint.hintColor = getNextLineColor()

        postInvalidate()
    }


    private var wordSearched = 0
    private var totalWordToSearch = 0


    fun startWordSearch(wordsName: Array<String>, xLen: Int, yLen: Int) {

        currentGridLength = IntVector2(xLen, yLen)

        if (viewSize.x == 0 || viewSize.y == 0) {
            callWordSearchFunctionOnMeasure = true
            tempWordNamesArray = wordsName

            // kill process
            return
        }

        hintsArray.clear()
        hintedWordsArray.clear()

        isTimeOut = false
        wordSearched = 0

        gridSize = viewSize.x.toFloat() / currentGridLength.x
        halfGridSize = gridSize / 2

        textPaint.textSize = halfGridSize
        linePaint.strokeWidth = textPaint.textSize + 15

        lineRadius = linePaint.strokeWidth / 2

//        if (gridSize * currentGridLength.y > viewSize.y) {
//            currentGridLength.y -= 1
//        }

        parentOffset.x = halfGridSize
        parentOffset.y = halfGridSize

        minLinePos = gridToPos(IntVector2(0, 0))
        maxLinePos = gridToPos(IntVector2(currentGridLength.x - 1, currentGridLength.y - 1))

        addWordsInWordSearch(wordsName)

        setMeasuredDimension(viewSize.x, (gridSize * currentGridLength.y).toInt())

    }

    // ------------------------ Add Words -------------------------

    private var allGridPosArray = ArrayList<IntVector2>()
    private var words = ArrayList<WordData>()
    private var searchableGridPosArray = ArrayList<IntVector2>()


    private fun addAllGridsInArray() {
        for (y: Int in 0 until currentGridLength.y) {
            for (x: Int in 0 until currentGridLength.x) {

                allGridPosArray.add(IntVector2(x, y))
            }
        }
    }

    private fun addWordsInWordSearch(wordsName: Array<String>) {

        addAllGridsInArray()
        gridData = GridData(currentGridLength)


        words.clear()
        for (name in wordsName) {
            words.add(WordData(name.uppercase()))
        }


        val searchableWords = ArrayList<WordData>()

        for (word: WordData in words) {

            var isAdded = false
            searchableGridPosArray = ArrayList(allGridPosArray)

            var loopTimes = 0

            while (searchableGridPosArray.size > 0) {
                loopTimes += 1

                // get random searchable grid pos
                val randomIndex = (0 until searchableGridPosArray.size).shuffled().last()
                val randomGridSearchPos = searchableGridPosArray[randomIndex]

                // find word search direction
                val wordSearchPattern = findPatternOfWordSearch(randomGridSearchPos, word.wordName)
                if (wordSearchPattern.x == 0 && wordSearchPattern.y == 0) {
                    // do nothing
                } else {
                    setGridDataUsingPattern(randomGridSearchPos, wordSearchPattern, word)

                    // temp
                    Log.e("===============", "Word added: $word")
                    searchableWords.add(word)
                    isAdded = true
                    break
                }


                // remove from array
                searchableGridPosArray.removeAt(randomIndex)
            }

            if (!isAdded) {
                Log.e("Not added: ", word.wordName)
//                removableWords.add(word)
            }
        }

        words = searchableWords

        // setting words color
//        var looped = 0
//        for (word in words){
//
//            if(looped == lineColors.size){
//                looped = 0
//            }
//
//            word.wordColor = lineColors[looped]
//        }

        totalWordToSearch = words.size

        val wordsArray = Array<String>(words.size) { "" }

        for (index in words.indices) {
            wordsArray[index] = words[index].wordName
        }


        onSetUpWordsListener?.onSetUpWordsListener(wordsArray)
        drawnLineDataArray.clear()

        setRandomCharOnEmptyPos()
    }

    private fun setRandomCharOnEmptyPos() {

        for (y: Int in 0 until currentGridLength.y) {
            for (x: Int in 0 until currentGridLength.x) {

                // if is pos data empty
                if (gridData.getGridData(IntVector2(x, y)) == '@') {

                    gridData.setGridData(IntVector2(x, y), ('A'..'Z').random())
                }
            }
        }
    }


    private val allSearchableDirectionArray = arrayListOf(
        WordSearchDirection.Top,
        WordSearchDirection.TopRight,
        WordSearchDirection.Right,
        WordSearchDirection.RightBottom,
        WordSearchDirection.Bottom,
        WordSearchDirection.LeftBottom,
        WordSearchDirection.Left,
        WordSearchDirection.TopLeft
    )
    var searchableDirectionArray = ArrayList<WordSearchDirection>()

    private fun findPatternOfWordSearch(gridStartSearchPos: IntVector2, word: String): IntVector2 {

        // if word first char match with the position grid char | position grid char is empty
        if (gridData.getGridData(gridStartSearchPos) == word[0] || gridData.getGridData(
                gridStartSearchPos
            ) == '@'
        ) {

            searchableDirectionArray = ArrayList(allSearchableDirectionArray)
            var randomDirectionIndex: Int
            val wordLength = word.length

            while (searchableDirectionArray.size > 0) {
                randomDirectionIndex = (0 until searchableDirectionArray.size).shuffled().last()

                when (searchableDirectionArray[randomDirectionIndex]) {

                    WordSearchDirection.Top -> {
                        val maxPossibleLength: Int = gridStartSearchPos.y + 1
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(0, -1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.TopRight -> {
                        val maxPossibleLength: Int = min(
                            currentGridLength.x - gridStartSearchPos.x,
                            gridStartSearchPos.y + 1
                        )
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(1, -1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.Right -> {
                        val maxPossibleLength: Int = currentGridLength.x - gridStartSearchPos.x
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(1, 0)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.RightBottom -> {
                        val maxPossibleLength: Int = min(
                            currentGridLength.x - gridStartSearchPos.x,
                            currentGridLength.y - gridStartSearchPos.y
                        )
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(1, 1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.Bottom -> {
                        val maxPossibleLength: Int = currentGridLength.y - gridStartSearchPos.y
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(0, 1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.LeftBottom -> {
                        val maxPossibleLength: Int = min(
                            gridStartSearchPos.x + 1,
                            currentGridLength.y - gridStartSearchPos.y
                        )
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(-1, 1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.Left -> {
                        val maxPossibleLength: Int = gridStartSearchPos.x + 1
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(-1, 0)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }
                    WordSearchDirection.TopLeft -> {
                        val maxPossibleLength: Int =
                            min(gridStartSearchPos.x + 1, gridStartSearchPos.y + 1)
                        if (wordLength <= maxPossibleLength) {

                            val pattern = IntVector2(-1, -1)
                            // if true
                            if (isWordPlaceable(gridStartSearchPos, pattern, word)) {
                                return pattern
                            }
                        }
                        break
                    }

                    else -> {}
                }

                searchableDirectionArray.removeAt(randomDirectionIndex)
            }
        }

        return IntVector2()
    }

    private fun isWordPlaceable(startPos: IntVector2, pattern: IntVector2, word: String): Boolean {

        val tempGridPos = startPos.copy()
        for (index in word.indices) {

            if (gridData.getGridData(tempGridPos) != '@' && gridData.getGridData(tempGridPos) != word[index]) {

                return false
            }

            tempGridPos.x = tempGridPos.x + pattern.x
            tempGridPos.y = tempGridPos.y + pattern.y
        }
        return true
    }

    private fun setGridDataUsingPattern(startPos: IntVector2, pattern: IntVector2, word: WordData) {

        word.startGrid = startPos
        val tempGridPos = startPos.copy()
        for (index in word.wordName.indices) {
            gridData.setGridData(tempGridPos, word.wordName[index])

            if (index == word.wordName.length - 1) {
                word.endGrid = tempGridPos
                break
            }

            tempGridPos.x = tempGridPos.x + pattern.x
            tempGridPos.y = tempGridPos.y + pattern.y
        }
    }


    private var drawnLineDataArray = ArrayList<DrawnLineData>()
    private var currentDrawingLine = DrawnLineData(FloatVector2(), FloatVector2(), 0)
    private var isCurrentLineDrawing = false

    var lineDrawnNum = 0

    override fun onDraw(canvas: Canvas) {

        lineDrawnNum = 0

        for (index in drawnLineDataArray.indices) {

            linePaint.color = ContextCompat.getColor(context, drawnLineDataArray[index].lineColor)

            canvas.drawLine(
                drawnLineDataArray[index].startPos.x,
                drawnLineDataArray[index].startPos.y,
                drawnLineDataArray[index].endPos.x,
                drawnLineDataArray[index].endPos.y,
                linePaint
            )

            lineDrawnNum++

            if (lineDrawnNum == lineColors.size) {
                lineDrawnNum = 0
            }

        }

        // draw current drawing line
        if (isCurrentLineDrawing) {

            if (lineDrawnNum == lineColors.size) {
                lineDrawnNum = 0
            }
            linePaint.color = ContextCompat.getColor(context, currentDrawingLine.lineColor)
            canvas.drawLine(
                currentDrawingLine.startPos.x,
                currentDrawingLine.startPos.y,
                currentDrawingLine.endPos.x,
                currentDrawingLine.endPos.y,
                linePaint
            )
        }

        // hints
        for (hint in hintsArray) {
            linePaint.color = ContextCompat.getColor(context, hint.hintColor)
            canvas.drawLine(
                hint.hintPos.x,
                hint.hintPos.y,
                hint.hintPos.x,
                hint.hintPos.y,
                linePaint
            )
        }

        // remove hints
        for (hint in removeHintsArray) {
            val color = ContextCompat.getColor(context, hint.hintColor)
            linePaint.color = Color.argb(hintsAlpha, color.red, color.green, color.blue)
            canvas.drawLine(
                hint.hintPos.x,
                hint.hintPos.y,
                hint.hintPos.x,
                hint.hintPos.y,
                linePaint
            )
        }

        createTextGrids(canvas)
    }

    private var searchWordDirection = WordSearchDirection.Empty

    private lateinit var minLinePos: FloatVector2
    private lateinit var maxLinePos: FloatVector2

    private fun calculateXDis(startXPos: Float, endXPos: Float): Float {

        var xDis = endXPos - startXPos

        if (endXPos < minLinePos.x) {
            xDis = -startXPos + parentOffset.x

        } else if (endXPos > maxLinePos.x) {
            xDis = maxLinePos.x - startXPos

        }

        return xDis
    }

    private fun calculateYDis(startYPos: Float, endYPos: Float): Float {

        var yDis = endYPos - startYPos

        if (endYPos < minLinePos.y) {
            yDis = -startYPos + parentOffset.y


        } else if (endYPos > maxLinePos.y) {
            yDis = maxLinePos.y - startYPos

        }

        return yDis
    }

    lateinit var searchTextView: TextView

    private lateinit var errorAlphaAnimation: ValueAnimator
    private lateinit var errorShakeAnimation: ValueAnimator

    private val translateMax = dpToFloat(10f)
    private fun setWordSearchErrorAnimation() {

        errorAlphaAnimation = ValueAnimator.ofInt(100, 0)

        errorAlphaAnimation.duration = 350
        errorAlphaAnimation.addUpdateListener {
            val value = it.animatedValue as Int
            searchTextView.alpha = value / 100f

        }

        errorAlphaAnimation.addListener(object : Animator.AnimatorListener {

            override fun onAnimationStart(p0: Animator) {}

            override fun onAnimationEnd(p0: Animator) {
                searchTextView.visibility = INVISIBLE

            }

            override fun onAnimationCancel(p0: Animator) {}

            override fun onAnimationRepeat(p0: Animator) {}

        })

        errorAlphaAnimation.start()


        // shake animation
        searchTextView.translationX = 0f

        errorShakeAnimation = ValueAnimator.ofFloat(1f, -1f)

        errorShakeAnimation.repeatCount = 3
        errorShakeAnimation.repeatMode = ValueAnimator.REVERSE
        errorShakeAnimation.duration = 100
        errorShakeAnimation.addUpdateListener {
            val value = it.animatedValue as Float
            searchTextView.translationX = translateMax * value

        }

        errorShakeAnimation.start()

    }

    private lateinit var correctAnimation: ValueAnimator
    private fun setWordSearchCorrectAnimation() {

        correctAnimation = ValueAnimator.ofInt(100, 0)

        correctAnimation.duration = 300
        correctAnimation.addUpdateListener {
            val value = it.animatedValue as Int
            searchTextView.alpha = value / 100f

        }


        correctAnimation.addListener(object : Animator.AnimatorListener {

            override fun onAnimationStart(p0: Animator) {}

            override fun onAnimationEnd(p0: Animator) {
                searchTextView.visibility = INVISIBLE

            }

            override fun onAnimationCancel(p0: Animator) {}

            override fun onAnimationRepeat(p0: Animator) {}

        })

        correctAnimation.start()

    }

    var isTimeOut = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if(isTimeOut){
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                if (this::errorAlphaAnimation.isInitialized) {
                    if (errorAlphaAnimation.isRunning) {
                        errorAlphaAnimation.cancel()
                        errorShakeAnimation.cancel()
                    }
                }

                // remove hints
                for (hint in hintsArray){
                    removeHintsArray.add(hint)
                }

                if(hintedWordsArray.size > 0){
                    hintedWordsArray.clear()
                    hintsArray.clear()
                    removeHints(this)
                }

                searchTextView.visibility = VISIBLE
                searchTextView.alpha = 1f
                searchWordDirection = WordSearchDirection.Empty
                isCurrentLineDrawing = true
                currentDrawingLine.lineColor = getNextLineColor()
                currentDrawingLine.startPos = gridToPos(posToGrid(FloatVector2(event.x, event.y)))
                currentDrawingLine.endPos = gridToPos(posToGrid(FloatVector2(event.x, event.y)))

            }

            MotionEvent.ACTION_MOVE -> {

                var xDis = calculateXDis(currentDrawingLine.startPos.x, event.x)
                var yDis = calculateYDis(currentDrawingLine.startPos.y, event.y)
                val absXDis = abs(xDis)
                val absYDis = abs(yDis)
                var ratio = 0f

                if (absXDis == 0f && absYDis == 0f) {
                    ratio = 0f

                } else if (absXDis < absYDis) {
                    ratio = absXDis / absYDis

                } else if (absYDis < absXDis) {

                    ratio = absYDis / absXDis
                }


                // line movement
                if (ratio > 0.5) { // diagonal

                    if (absXDis < absYDis) {
                        yDis = yDis / absYDis * absXDis
                    } else {
                        xDis = xDis / absXDis * absYDis
                    }

                    currentDrawingLine.endPos = FloatVector2(
                        currentDrawingLine.startPos.x + xDis,
                        currentDrawingLine.startPos.y + yDis
                    )

                } else {

                    if (absXDis > absYDis) { // horizontal
                        currentDrawingLine.endPos = FloatVector2(
                            currentDrawingLine.startPos.x + xDis,
                            currentDrawingLine.startPos.y
                        )

                    } else if (absYDis > absXDis) { // vertical
                        currentDrawingLine.endPos = FloatVector2(
                            currentDrawingLine.startPos.x,
                            currentDrawingLine.startPos.y + yDis
                        )

                    }

                }
            }

            MotionEvent.ACTION_UP -> {

                isCurrentLineDrawing = false

                val startGrid = posToGrid(currentDrawingLine.startPos)
                val endGrid = wordSearchEndPosToGrid(startGrid)
                val searchedWord = getWordFromIndex(startGrid, endGrid)

                onSearchValueChangeListener?.onSearchValueChangeListener(searchedWord)

                if (onValueSearchedListener?.onValueSearchedListener(searchedWord) == true) {

                    drawnLineDataArray.add(
                        DrawnLineData(
                            currentDrawingLine.startPos,
                            gridToPos(endGrid),
                            currentDrawingLine.lineColor
                        )
                    )
                    setWordSearchCorrectAnimation()

                    for (word in words){
                        if(searchedWord == word.wordName){
                            word.isSearched = true
                            break
                        }
                    }

                    wordSearched += 1

                    // check if all words are searched
                    if (wordSearched >= totalWordToSearch) {

                        // level completed
                        onLevelCompleted?.levelCompleted()

                    }

                } else {
                    setWordSearchErrorAnimation()

                }

                postInvalidate()
                return true

            }

            else -> return false

        }

        val startGrid = posToGrid(currentDrawingLine.startPos)
        onSearchValueChangeListener?.onSearchValueChangeListener(
            getWordFromIndex(
                startGrid,
                wordSearchEndPosToGrid(startGrid)
            )
        )

        postInvalidate()
        return true
    }

    private fun wordSearchEndPosToGrid(startGrid: IntVector2): IntVector2 {

        var xDis = currentDrawingLine.endPos.x - currentDrawingLine.startPos.x
        var yDis = currentDrawingLine.endPos.y - currentDrawingLine.startPos.y

        if (xDis < 0) {
            xDis -= lineRadius
        } else {
            xDis += lineRadius
        }

        if (yDis < 0) {
            yDis -= lineRadius
        } else {
            yDis += lineRadius
        }

        val xGrid = (xDis / gridSize).toInt() + startGrid.x
        val yGrid = (yDis / gridSize).toInt() + startGrid.y

        return IntVector2(xGrid, yGrid)

    }


    private fun getWordFromIndex(startGrid: IntVector2, endGrid: IntVector2): String {

        val pattern = IntVector2()
        val xDis = endGrid.x - startGrid.x
        val yDis = endGrid.y - startGrid.y

        var wordLength = 1

        if (xDis < 0 && yDis > 0) { // top-left
            pattern.x = -1
            pattern.y = 1
            wordLength += yDis

        } else if (xDis == 0 && yDis > 0) { // top
            pattern.x = 0
            pattern.y = 1
            wordLength += yDis

        } else if (xDis > 0 && yDis > 0) { // top-right
            pattern.x = 1
            pattern.y = 1
            wordLength += yDis

        } else if (xDis > 0 && yDis == 0) { // right
            pattern.x = 1
            pattern.y = 0
            wordLength += xDis

        } else if (xDis > 0 && yDis < 0) { // bottom-right
            pattern.x = 1
            pattern.y = -1
            wordLength += xDis

        } else if (xDis == 0 && yDis < 0) { // bottom
            pattern.x = 0
            pattern.y = -1
            wordLength += abs(yDis)

        } else if (xDis < 0 && yDis < 0) { // left-bottom
            pattern.x = -1
            pattern.y = -1
            wordLength += abs(yDis)

        } else if (xDis < 0 && yDis == 0) { // left
            pattern.x = -1
            pattern.y = 0
            wordLength += abs(xDis)
        }

        var word = ""

        for (loop in 1..wordLength) {

            word += gridData.getGridData(startGrid)

            startGrid.x += pattern.x
            startGrid.y += pattern.y
        }
        word.reversed()

        return word
    }

    private fun createTextGrids(canvas: Canvas) {

        var pos: FloatVector2
        for (y: Int in 0 until currentGridLength.y) {
            for (x: Int in 0 until currentGridLength.x) {

                val text = gridData.getGridData(IntVector2(x, y)).toString()
                val bounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)

                pos = gridToPos(IntVector2(x, y))
                canvas.drawText(text, pos.x, pos.y - bounds.centerY(), textPaint)

                // show grid help
//                canvas.drawRect(x*gridSize, y*gridSize, x*gridSize + gridSize, y*gridSize + gridSize, outlinePaint)
            }
        }
    }


    private fun posToGrid(pos: FloatVector2): IntVector2 {
        return IntVector2(floor(pos.x / gridSize).toInt(), floor(pos.y / gridSize).toInt())
    }

    private fun gridToPos(gridPos: IntVector2): FloatVector2 {
        return FloatVector2(
            gridPos.x * gridSize + parentOffset.x,
            gridPos.y * gridSize + parentOffset.y
        )
    }

    private fun dpToFloat(dp: Float): Float {

        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt().toFloat()
    }


    // ------------ on search value change listener

    // 1
    private var onSearchValueChangeListener: OnSearchValueChangeListener? = null

    interface OnSearchValueChangeListener {
        fun onSearchValueChangeListener(searchedValue: String)
    }

    fun setOnSearchValueChangeListener(onSearchValueChangeListener: OnSearchValueChangeListener) {
        this.onSearchValueChangeListener = onSearchValueChangeListener
    }

    // 2
    private var onValueSearchedListener: OnValueSearchedListener? = null

    interface OnValueSearchedListener {
        fun onValueSearchedListener(searchedValue: String): Boolean
    }

    fun setOnValueSearchedListener(onValueSearchedListener: OnValueSearchedListener) {
        this.onValueSearchedListener = onValueSearchedListener
    }

    // 3
    private var onSetUpWordsListener: OnSetUpWordsListener? = null

    interface OnSetUpWordsListener {
        fun onSetUpWordsListener(wordsArray: Array<String>)
    }

    fun setOnSetUpWordsListener(onSetUpWordsListener: OnSetUpWordsListener) {
        this.onSetUpWordsListener = onSetUpWordsListener
    }

    // 4
    private var onLevelCompleted: OnLevelComplete? = null

    interface OnLevelComplete {
        fun levelCompleted()
    }

    fun setOnLevelComplete(onLevelCompleted: OnLevelComplete) {
        this.onLevelCompleted = onLevelCompleted
    }

}

data class FloatVector2(var x: Float = 0f, var y: Float = 0f)
data class IntVector2(var x: Int = 0, var y: Int = 0)

data class WordData(
    var wordName: String,
    var startGrid: IntVector2 = IntVector2(),
    var endGrid: IntVector2 = IntVector2(),
    var wordColor: Int = 0,
    var isSearched: Boolean = false
)

data class DrawnLineData(var startPos: FloatVector2, var endPos: FloatVector2, var lineColor: Int)

enum class WordSearchDirection { Empty, Top, TopRight, Right, RightBottom, Bottom, LeftBottom, Left, TopLeft }

class GridData(gridLength: IntVector2) {

    private val dataArray = Array(gridLength.x) { CharArray(gridLength.y) { '@' } }

    fun getGridData(pos: IntVector2): Char {
        return dataArray[pos.x][pos.y]
    }

    fun setGridData(pos: IntVector2, data: Char) {
        dataArray[pos.x][pos.y] = data
    }
}

data class Hint(val hintPos: FloatVector2, var hintColor: Int)