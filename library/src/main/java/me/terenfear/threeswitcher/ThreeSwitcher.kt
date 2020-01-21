package me.terenfear.threeswitcher

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Property
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.FloatRange
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import me.terenfear.threeswitcher.library.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Created by Terenfear on 21.01.2020.
 */
class ThreeSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // properties that can be modified from outside
    var state: State = State.fromCurrentProgress()
        private set
    var onStateChangedListener: OnStateChangedListener? = null

    var textLeft: String? = State.LEFT.toString()
        set(value) {
            field = value
            progressAnimator?.cancel()
            requestLayout()
            invalidate()
        }
    var textCenter: String? = State.CENTER.toString()
        set(value) {
            field = value
            progressAnimator?.cancel()
            requestLayout()
            invalidate()
        }
    var textRight: String? = State.RIGHT.toString()
        set(value) {
            field = value
            progressAnimator?.cancel()
            requestLayout()
            invalidate()
        }

    var textMarginLeft: Float = 10f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var textMarginRight: Float = 10f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var textMarginTop: Float = 10f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var textMarginBottom: Float = 10f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var textSize: Float = 40f
        set(value) {
            field = value
            textPaint.textSize = field
            textPaintHighlighted.textSize = field
            requestLayout()
            invalidate()
        }
    var shadowRadius: Float = 18f
        set(value) {
            field = value
            thumbPaint.setShadowLayer(field, 0f, 0f, shadowColor)
            requestLayout()
            invalidate()
        }
    var cornersRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var trackColor: Int = context.primaryColor
        set(value) {
            field = value
            trackPaint.color = field
            invalidate()
        }
    var thumbColor: Int = context.accentColor
        set(value) {
            field = value
            thumbPaint.color = field
            invalidate()
        }
    var textColor: Int = context.accentColor
        set(value) {
            field = value
            textPaint.color = field
            invalidate()
        }
    var textColorHighlighted: Int = context.primaryColor
        set(value) {
            field = value
            textPaintHighlighted.color = field
            invalidate()
        }
    var shadowColor: Int = Color.LTGRAY
        set(value) {
            field = value
            thumbPaint.setShadowLayer(shadowRadius, 0f, 0f, field)
            invalidate()
        }

    // inner properties
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val clickTimeout =
        ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()
    private var isCatchingScrolls = false

    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var touchLastX: Float = 0f

    private lateinit var textLeftLayout: Layout
    private lateinit var textCenterLayout: Layout
    private lateinit var textRightLayout: Layout
    private lateinit var textLeftHighlightedLayout: Layout
    private lateinit var textCenterHighlightedLayout: Layout
    private lateinit var textRightHighlightedLayout: Layout

    private var availableTextHeight: Float = 0f
    private var leftEmptySpace: Float = 0f
    private var rightEmptySpace: Float = 0f
    private var topEmptySpace: Float = 0f
    private var bottomEmptySpace: Float = 0f


    private val trackPaint = Paint().apply {
        style = Paint.Style.FILL
        color = trackColor
    }
    private val thumbPaint = Paint().apply {
        style = Paint.Style.FILL
        color = thumbColor
        setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
    }
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        textSize = this@ThreeSwitcher.textSize
        color = textColor
    }
    private val textPaintHighlighted = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        textSize = this@ThreeSwitcher.textSize
        color = textColorHighlighted
    }

    private val trackRect = RectF()
    private val thumbRect = RectF()

    private var progressAnimator: ObjectAnimator? = null

    @FloatRange(from = MIN_VIEW_PROGRESS, to = MAX_VIEW_PROGRESS)
    private var progress: Float = State.LEFT.medianProgress
        set(value) {
            field = value.coerceIn(MIN_VIEW_PROGRESS.toFloat(), MAX_VIEW_PROGRESS.toFloat())
            invalidate()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.ThreeSwitcher, defStyleAttr) {
            getInt(R.styleable.ThreeSwitcher_tw_state, state.ordinal)
                .let(State.values()::get)
                .let(this@ThreeSwitcher::setStateNoEventImmediately)

            textLeft = getString(R.styleable.ThreeSwitcher_tw_textLeft) ?: textLeft
            textCenter = getString(R.styleable.ThreeSwitcher_tw_textCenter) ?: textCenter
            textRight = getString(R.styleable.ThreeSwitcher_tw_textRight) ?: textRight

            textMarginLeft = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_textMarginLeft,
                textMarginLeft.toInt()
            ).toFloat()
            textMarginRight = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_textMarginRight,
                textMarginRight.toInt()
            ).toFloat()
            textMarginTop = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_textMarginTop,
                textMarginTop.toInt()
            ).toFloat()
            textMarginBottom = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_textMarginBottom,
                textMarginBottom.toInt()
            ).toFloat()
            textSize = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_textSize,
                textSize.toInt()
            ).toFloat()
            shadowRadius = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_shadowRadius,
                shadowRadius.toInt()
            ).toFloat()
            cornersRadius = getDimensionPixelSize(
                R.styleable.ThreeSwitcher_tw_cornersRadius,
                cornersRadius.toInt()
            ).toFloat()

            trackColor = getColor(R.styleable.ThreeSwitcher_tw_trackColor, trackColor)
            thumbColor = getColor(R.styleable.ThreeSwitcher_tw_thumbColor, thumbColor)
            textColor = getColor(R.styleable.ThreeSwitcher_tw_textColor, textColor)
            textColorHighlighted =
                getColor(R.styleable.ThreeSwitcher_tw_textColorHighlighted, textColorHighlighted)
            shadowColor = getColor(R.styleable.ThreeSwitcher_tw_shadowColor, shadowColor)
        }

        context.withStyledAttributes(
            attrs,
            intArrayOf(android.R.attr.focusable, android.R.attr.clickable),
            defStyleAttr
        ) {
            isFocusable = getBoolean(0, true)
            @SuppressLint("ResourceType")
            isClickable = getBoolean(1, isFocusable)
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updateTextLayouts()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(
            state,
            superState
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            setStateNoEventImmediately(state.switcherState)
            super.onRestoreInstanceState(state.superState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        updateTextLayouts()

        leftEmptySpace = shadowRadius + paddingLeft.toFloat()
        rightEmptySpace = shadowRadius + paddingRight.toFloat()
        topEmptySpace = shadowRadius + paddingTop.toFloat()
        bottomEmptySpace = shadowRadius + paddingBottom.toFloat()

        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {

            val trackWidth = width - leftEmptySpace - rightEmptySpace
            val trackHeight = height - topEmptySpace - bottomEmptySpace
            canvas.withTranslation(leftEmptySpace, topEmptySpace) {
                trackRect.set(0f, 0f, trackWidth, trackHeight)
                canvas.drawRoundRect(
                    trackRect,
                    cornersRadius,
                    cornersRadius,
                    trackPaint
                )
            }

            val textGap = textMarginLeft + textMarginRight
            val textWidthWithInnerGaps =
                (textLeftLayout.width + textCenterLayout.width + textRightLayout.width + textGap * 2)
            canvas.withSave {
                canvas.translate(leftEmptySpace + textMarginLeft, topEmptySpace + textMarginTop)
                canvas.clipRect(
                    0f,
                    0f,
                    textWidthWithInnerGaps,
                    availableTextHeight,
                    Region.Op.INTERSECT
                )
                canvas.translate(0f, (availableTextHeight - textLeftLayout.height) / 2)
                textLeftLayout.draw(this)
                canvas.translate(textLeftLayout.width.toFloat() + textGap, 0f)
                textCenterLayout.draw(this)
                canvas.translate(textCenterLayout.width.toFloat() + textGap, 0f)
                textRightLayout.draw(this)
            }

            val thumbWidth = trackWidth / 3f
            val thumbLeft =
                (trackWidth * progress - thumbWidth / 2).coerceIn(0f, trackWidth - thumbWidth)
            canvas.withTranslation(leftEmptySpace, topEmptySpace) {
                thumbRect.set(thumbLeft, 0f, thumbLeft + thumbWidth, trackHeight)
                canvas.drawRoundRect(
                    thumbRect,
                    cornersRadius,
                    cornersRadius,
                    thumbPaint
                )
            }

            canvas.withSave {
                canvas.translate(leftEmptySpace, topEmptySpace)
                // TODO (28/01/2020): clip round rect
                canvas.clipRect(
                    thumbLeft,
                    0f,
                    thumbLeft + thumbWidth,
                    trackHeight,
                    Region.Op.INTERSECT
                )

                canvas.translate(textMarginLeft, textMarginTop)
                canvas.clipRect(
                    0f,
                    0f,
                    textWidthWithInnerGaps,
                    availableTextHeight,
                    Region.Op.INTERSECT
                )
                canvas.translate(0f, (availableTextHeight - textLeftLayout.height) / 2)
                textLeftHighlightedLayout.draw(this)
                canvas.translate(textLeftHighlightedLayout.width.toFloat() + textGap, 0f)
                textCenterHighlightedLayout.draw(this)
                canvas.translate(textCenterHighlightedLayout.width.toFloat() + textGap, 0f)
                textRightHighlightedLayout.draw(this)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || !isClickable || !isFocusable) {
            return false
        }
        val action = event.action
        val deltaX: Float = event.x - touchStartX
        val deltaY: Float = event.y - touchStartY
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchLastX = touchStartX
                isPressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                progressAnimator?.cancel()
                val x = event.x
                progress += (x - touchLastX) / width
                // when delta reaches touchSlop, most likely the parent will intercept the event
                // and treat all further movement as scrolling. we don't want it, so we try to disallow
                // parent's interception. but only if movement is horizontal, vertical movement is
                // still can (and should) be intercepted
                // in short: horizontal scrolling won't work while we dragging, but vertical will work fine
                // TODO (21/01/2020): this is how I think it works, but I need to test whether this is correct
                if (!isCatchingScrolls && (abs(deltaX) > touchSlop / 2 || abs(deltaY) > touchSlop / 2)) {
                    if (deltaY == 0f || abs(deltaX) > abs(deltaY)) {
                        catchView()
                    } else if (abs(deltaY) > abs(deltaX)) {
                        return false
                    }
                }
                touchLastX = x
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                isCatchingScrolls = false
                isPressed = false
                val time = event.eventTime - event.downTime.toFloat()
                // detect whether movement is too short and too fast to be considered a scrolling event
                if (abs(deltaX) < touchSlop && abs(deltaY) < touchSlop && time < clickTimeout) {
                    // TODO (21/01/2020): get rid of duplicate code
                    val nextState =
                        State.fromProgress(
                            touchStartX / width
                        )
                    if (nextState != state) {
                        setState(nextState)
                    } else {
                        animateToState(nextState)
                    }
                    performClick()
                } else {
                    val nextState = State.fromCurrentProgress()
                    if (nextState != state) {
                        playSoundEffect(SoundEffectConstants.CLICK)
                        setState(nextState)
                    } else {
                        animateToState(nextState)
                    }
                }
            }
        }
        return true
    }

    fun setState(newState: State) {
        if (state != newState) {
            state = newState
            onStateChangedListener?.invoke(this, state)
            animateToState(state)
        }
    }

    fun setStateImmediately(newState: State) {
        if (state != newState) {
            state = newState
            onStateChangedListener?.invoke(this, state)
            progressAnimator?.cancel()
            progress = state.medianProgress
        }
    }

    fun setStateNoEvent(newState: State) {
        if (onStateChangedListener == null) {
            setState(newState)
        } else {
            val currentListener = onStateChangedListener
            onStateChangedListener = null
            setState(newState)
            onStateChangedListener = currentListener
        }
    }

    fun setStateNoEventImmediately(newState: State) {
        if (onStateChangedListener == null) {
            setStateImmediately(newState)
        } else {
            val currentListener = onStateChangedListener
            onStateChangedListener = null
            setStateImmediately(newState)
            onStateChangedListener = currentListener
        }
    }

    private fun updateTextLayouts(desiredWidth: Int? = null) {
        val width = desiredWidth ?: calcMaxTextLength()
        textLeftLayout = getTextLayout(textLeft, textPaint, width)
        textCenterLayout = getTextLayout(textCenter, textPaint, width)
        textRightLayout = getTextLayout(textRight, textPaint, width)
        textLeftHighlightedLayout = getTextLayout(textLeft, textPaintHighlighted, width)
        textCenterHighlightedLayout = getTextLayout(textCenter, textPaintHighlighted, width)
        textRightHighlightedLayout = getTextLayout(textRight, textPaintHighlighted, width)
    }

    private fun calcMaxTextLength(): Int {
        return max(
            calcTextWidth(textRight, textPaint),
            max(
                calcTextWidth(textLeft, textPaint),
                calcTextWidth(textCenter, textPaint)
            )
        )
    }

    private fun getTextLayout(text: String?, paint: TextPaint, desiredWidth: Int): Layout {
        val innerText = text.orEmpty()
        return StaticLayout(
            innerText,
            paint,
            desiredWidth,
            Layout.Alignment.ALIGN_CENTER,
            1f,
            0f,
            false
        )
    }

    private fun calcTextWidth(text: String?, paint: TextPaint): Int =
        if (text.isNullOrEmpty()) {
            0
        } else {
            ceil(Layout.getDesiredWidth(text, paint)).toInt()
        }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val emptySpaceWidth = calcEmptySpaceWidth()
        return if (widthMode == MeasureSpec.EXACTLY) {
            updateTextLayouts(((requestedWidth - emptySpaceWidth) / 3).toInt())
            requestedWidth
        } else {
            val calculatedWidth: Int = (textLeftLayout.width * 3 + emptySpaceWidth).toInt()
            if (widthMode == MeasureSpec.AT_MOST && calculatedWidth > requestedWidth) {
                updateTextLayouts(((requestedWidth - emptySpaceWidth) / 3).toInt())
                requestedWidth
            } else {
                calculatedWidth
            }
        }
    }

    private fun calcEmptySpaceWidth() =
        paddingLeft + paddingRight + textMarginLeft * 3 + textMarginRight * 3 + shadowRadius * 2

    private fun measureHeight(heightMeasureSpec: Int): Int {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)
        val emptySpaceHeight = calcEmptySpaceHeight()
        val textPaintHeight = getTextPaintHeight()
        return if (heightMode == MeasureSpec.EXACTLY) {
            availableTextHeight = (requestedHeight - emptySpaceHeight).coerceAtLeast(0f)
            requestedHeight
        } else {
            val calculatedHeight: Int = (textPaintHeight + emptySpaceHeight).toInt()
            if (heightMode == MeasureSpec.AT_MOST && calculatedHeight > requestedHeight) {
                availableTextHeight =
                    (requestedHeight - emptySpaceHeight).coerceAtLeast(0f)
                requestedHeight
            } else {
                availableTextHeight = textPaintHeight
                calculatedHeight
            }
        }
    }

    private fun calcEmptySpaceHeight() =
        paddingTop + paddingBottom + textMarginTop + textMarginBottom + shadowRadius * 2

    private fun getTextPaintHeight(): Float = (-textPaint.ascent() + textPaint.descent())

    private fun animateToState(state: State) {
        progressAnimator = ObjectAnimator.ofFloat(this,
            THUMB_POS, state.medianProgress)
            .apply {
                setAutoCancel(true)
                start()
            }
    }

    private fun catchView() {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(true)
        isCatchingScrolls = true
    }

    internal class SavedState : BaseSavedState {
        val switcherState: State

        constructor(switcherState: State, superState: Parcelable?) : super(superState) {
            this.switcherState = switcherState
        }

        private constructor(input: Parcel) : super(input) {
            switcherState = State.values()[input.readInt()]
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(switcherState.ordinal)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(
                    parcel
                )
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val MIN_VIEW_PROGRESS = 0.0
        const val MAX_VIEW_PROGRESS = 1.0
        const val THIRD_OF_VIEW_PROGRESS = (MAX_VIEW_PROGRESS - MIN_VIEW_PROGRESS) / 3
        private val THUMB_POS: Property<ThreeSwitcher, Float> =
            object : Property<ThreeSwitcher, Float>(Float::class.java, "progress") {
                override fun get(switch: ThreeSwitcher): Float {
                    return switch.progress
                }

                override fun set(switch: ThreeSwitcher, value: Float) {
                    switch.progress = value
                }
            }
    }

    enum class State(
        @FloatRange(from = MIN_VIEW_PROGRESS, to = MAX_VIEW_PROGRESS) val maxProgress: Float
    ) {
        LEFT(THIRD_OF_VIEW_PROGRESS.toFloat()),
        CENTER((2 * THIRD_OF_VIEW_PROGRESS).toFloat()),
        RIGHT(MAX_VIEW_PROGRESS.toFloat());

        val medianProgress: Float = (maxProgress - THIRD_OF_VIEW_PROGRESS / 2).toFloat()

        companion object {
            fun fromProgress(
                @FloatRange(
                    from = MIN_VIEW_PROGRESS,
                    to = MAX_VIEW_PROGRESS
                ) progress: Float
            ) =
                values().minBy { abs(it.medianProgress - progress) }!!
        }
    }

    private fun State.Companion.fromCurrentProgress() =
        fromProgress(
            progress
        )
}

typealias OnStateChangedListener = (view: View, newState: ThreeSwitcher.State) -> Unit