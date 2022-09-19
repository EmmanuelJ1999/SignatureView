package com.patodev.signature

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val TAG = this::class.simpleName
    private var _bitmap: Bitmap? = null
    private var _canvas: Canvas? = null
    private val _path: Path = Path()
    private val _bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private val _paint: Paint = Paint()
    private var _mX = 0f
    private var _mY = 0f
    private val touchTolerance = 4f
    var penSize = 4f
    var penColor = 0
    var backgroundCanvas = 0
    var enableSignature = true

    init {
        val typedArray = getContext().theme.obtainStyledAttributes(
            attrs, R.styleable.signature,0,0
        )
        try{
            penSize = typedArray.getDimension(
                R.styleable.signature_penSize,
                //context.resources.getDimension(R.dimen.pen_size)
                penSize
            )
            penColor = typedArray.getColor(
                R.styleable.signature_penColor,
                ContextCompat.getColor(context, R.color.black)
            )
            backgroundCanvas = typedArray.getColor(
                R.styleable.signature_backgroundCanvas,
                ContextCompat.getColor(context, R.color.white)
            )
            enableSignature = typedArray.getBoolean(
                R.styleable.signature_enableSignature,
                true
            )
        }catch(e: Exception){
            Log.e(TAG,"Error: " + e.message)
        }
        finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        _paint.isAntiAlias = true
        _paint.isDither = true
        _paint.color = penColor
        _paint.style = Paint.Style.STROKE
        _paint.strokeJoin = Paint.Join.ROUND
        _paint.strokeCap = Paint.Cap.ROUND
        _paint.strokeWidth = penSize
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        _bitmap = Bitmap.createBitmap(
            w,
            if (h > 0) h else (parent as View).height,
            Bitmap.Config.ARGB_8888
        )
        _canvas = Canvas(_bitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundCanvas)
        canvas.drawBitmap(_bitmap!!, 0f, 0f, _bitmapPaint)
        canvas.drawPath(_path, _paint)
    }

    private fun touchStart(x: Float, y: Float) {
        _path.reset()
        _path.moveTo(x, y)
        _mX = x
        _mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - _mX)
        val dy = abs(y - _mY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            _path.quadTo(_mX, _mY, (x + _mX) / 2, (y + _mY) / 2)
            _mX = x
            _mY = y
        }
    }

    private fun touchUp() {
        if (!_path.isEmpty) {
            _path.lineTo(_mX, _mY)
            _canvas!!.drawPath(_path, _paint)
        } else {
            _canvas!!.drawPoint(_mX, _mY, _paint)
        }
        _path.reset()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        super.onTouchEvent(e)
        if(!enableSignature)
            return false

        val x = e.x
        val y = e.y
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    fun clearCanvas() {
        _canvas!!.drawColor(backgroundCanvas)
        invalidate()
    }

    val bytes: ByteArray
        get() {
            val b = bitmap
            val baos = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        }

    val bitmap: Bitmap
        get() {
            val v = this.parent as View
            val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            v.layout(v.left, v.top, v.right, v.bottom)
            v.draw(c)
            return b
        }
}