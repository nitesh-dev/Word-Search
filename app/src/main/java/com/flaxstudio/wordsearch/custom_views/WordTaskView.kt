package com.flaxstudio.wordsearch.custom_views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import com.flaxstudio.wordsearch.R
import java.security.AccessController.getContext

class WordTaskView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var words = arrayListOf<WordBound>()

    init {
        words.add(WordBound("DIVING"))
        words.add(WordBound("JUDO"))
        words.add(WordBound("HOCKEY"))
//        words.add(WordBound("RUGBY"))
//        words.add(WordBound("SOCCER"))
    }

    fun startWordTaskView(rawWords: Array<String>){

        words.clear()
        for (rawWord in rawWords){
            words.add(WordBound(rawWord))
        }

        calculateWordsPos()

    }

    private val parentMargin = 20
    private var viewSize = IntVector2()

    private var tempWordsBound = ArrayList<WordBound>()

    private fun calculateWordsPos() {

        val parentInnerWidth = viewSize.x - (2 * parentMargin)

        var totalWidth = 0f
        var totalHeight = 0f

        var isCentered = false

        // setting text position

        var wordIndex = 0

        while (wordIndex < words.size){

            isCentered = false
            totalWidth += words[wordIndex].childWidth

            if (totalWidth < parentInnerWidth) {
                words[wordIndex].setTextPos(
                    totalWidth - words[wordIndex].childWidth + parentMargin,
                    totalHeight + parentMargin
                )

                // store temp
                tempWordsBound.add(words[wordIndex])
                wordIndex++

            } else {
                isCentered = true

                // calculating relative position
                val relative = parentInnerWidth / 2 - (totalWidth - words[wordIndex].childWidth) / 2

                for (word in tempWordsBound) {
                    word.setTextPos(word.textPos.x + relative, word.textPos.y)
                }
                tempWordsBound.clear()

                // next
                totalHeight += words[wordIndex].childHeight
                totalWidth = 0f

            }
        }


        if (!isCentered) {
            // calculating relative position
            val relative = parentInnerWidth / 2 - totalWidth / 2

            for (word in tempWordsBound) {
                word.setTextPos(word.textPos.x + relative, word.textPos.y)
            }
            tempWordsBound.clear()

            totalHeight += words.last().childHeight
        }


        // set parent view height
        setMeasuredDimension(viewSize.x, (totalHeight + 2 * parentMargin).toInt())

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        viewSize.x = MeasureSpec.getSize(widthMeasureSpec)
        viewSize.y = MeasureSpec.getSize(heightMeasureSpec)

        calculateWordsPos()

    }

    override fun onDraw(canvas: Canvas) {

        for (word in words) {

            word.drawText(canvas)
//            word.showBound(canvas)
        }

    }

    fun matchSearchedWord(wordSearched: String): Boolean {

        for (index in words.indices){

            if(!words[index].isSearched){  // not searched
                if(words[index].text == wordSearched){
                    words[index].isSearched = true
                    words[index].startAnimation(this)

                    return true
                }
            }
        }

        return false
    }


    inner class WordBound(text: String) {

        var isSearched = false
        var isWordInWordSearch = false

        private val outlinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
        }

        private val strikePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private var textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 35f
            typeface = ResourcesCompat.getFont(context, R.font.jellee_bold_700)

        }

        var childMargin = 20f
        var childTotalMargin = childMargin * 2

        var text = ""
        var textBound: Rect = Rect()
        var childWidth = 0f
        var childHeight = 0f

        private var isAnimating = false

        private val strikethroughStartPos = FloatVector2()


        init {
            this.text = text
            textPaint.getTextBounds(text, 0, text.length, textBound)

            childWidth = textBound.width() + childTotalMargin
            childHeight = textBound.height() + childTotalMargin

        }


        var textPos = FloatVector2()
        var animationValue = 0f

        fun setTextPos(x: Float, y: Float) {

            textPos.x = x
            textPos.y = y

            strikethroughStartPos.x = textPos.x + childMargin / 2
            strikethroughStartPos.y = textPos.y + childMargin - textBound.centerY()

        }

        private val textMinOpacity = 0.3f
        private val strikeMinOpacity = 0.5f
        fun drawText(canvas: Canvas) {

            if (isAnimating) {

                textPaint.color = Color.argb((((textMinOpacity * 255) - 255) * animationValue + 255).toInt(), 255, 255, 255)

                canvas.drawText(
                    text,
                    textPos.x + childMargin,
                    textPos.y + childMargin + textBound.height(),
                    textPaint
                )

                strikePaint.color = Color.argb((((strikeMinOpacity * 255) - 255) * animationValue + 255).toInt(), 255, 255, 255)
                canvas.drawLine(
                    strikethroughStartPos.x,
                    strikethroughStartPos.y,
                    strikethroughStartPos.x + (textBound.width() + childMargin) * animationValue,
                    strikethroughStartPos.y,
                    strikePaint
                )

            } else {

                canvas.drawText(
                    text,
                    textPos.x + childMargin,
                    textPos.y + childMargin + textBound.height(),
                    textPaint
                )
            }
        }

        fun startAnimation(view: WordTaskView) {
            isAnimating = true

            // animating
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 200
                interpolator = LinearInterpolator()
                addUpdateListener { valueAnimator ->
                    animationValue = valueAnimator.animatedValue as Float
                    view.invalidate()
                }


            }.start()
        }

        fun showBound(canvas: Canvas) {
            canvas.drawRect(
                textPos.x,
                textPos.y,
                textPos.x + childWidth,
                textPos.y + childHeight,
                outlinePaint
            )

        }

//        fun clone(): WordBound{
//            return WordBound(text)
//        }
    }
}