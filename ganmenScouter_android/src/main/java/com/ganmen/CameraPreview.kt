package com.ganmen

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class CameraPreview(context: Context?, private val mCam: Camera) : SurfaceView(context), SurfaceHolder.Callback {

    /**
     * SurfaceView 生成
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            // カメラインスタンスに、画像表示先を設定
            mCam.setPreviewDisplay(holder)
            // プレビュー開始
            mCam.startPreview()
        } catch (e: IOException) {
            //
        }
    }

    /**
     * SurfaceView 破棄
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    /**
     * SurfaceHolder が変化したときのイベント
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 画面回転に対応する場合は、ここでプレビューを停止し、
        // 回転による処理を実施、再度プレビューを開始する。
    }

    /**
     * コンストラクタ
     */
    init {

        // サーフェスホルダーの取得とコールバック通知先の設定
        val holder = holder
        holder.addCallback(this)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
}