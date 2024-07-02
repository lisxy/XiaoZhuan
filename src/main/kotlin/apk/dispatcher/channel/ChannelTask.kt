package apk.dispatcher.channel

import apk.dispatcher.log.AppLogger
import apk.dispatcher.util.ApkInfo
import apk.dispatcher.util.getApkInfo
import java.io.File
import kotlin.jvm.Throws

abstract class ChannelTask {

    abstract val channelName: String


    private var submitStateListener: SubmitStateListener? = null

    /**
     * 声明需要的参数
     */
    protected abstract val paramDefine: List<Param>

    /**
     * 文件名标识
     */
    abstract val fileNameIdentify: String


    /**
     * 初始化参数
     */
    abstract fun init(params: Map<Param, String?>)

    /**
     * 添加监听器
     */
    fun setSubmitStateListener(listener: SubmitStateListener) {
        this.submitStateListener = listener
    }

    fun getParams(): List<Param> {
        val fileName = Param(FILE_NAME_IDENTIFY, fileNameIdentify, "文件名标识,不区分大小写")
        return paramDefine + fileName
    }

    @kotlin.jvm.Throws
    suspend fun startUpload(apkFile: File, updateDesc: String) {
        AppLogger.info(LOG_TAG, "开始上传")
        submitStateListener?.onProcessing("请求中")
        try {
            submitStateListener?.onStart()
            performUpload(apkFile, getApkInfo(apkFile), updateDesc) {
                if (it == 100) {
                    submitStateListener?.onProcessing("提交中")
                } else {
                    submitStateListener?.onProgress(it)
                }
            }
            submitStateListener?.onSuccess()
            AppLogger.info(LOG_TAG, "上传成功")
        } catch (e: Exception) {
            AppLogger.error(LOG_TAG, "上传失败", e)
            submitStateListener?.onError(e)
        }
    }

    /**
     * 执行结束，表示上传成功，抛出异常代表出错
     */
    @Throws
    abstract suspend fun performUpload(file: File, apkInfo: ApkInfo, updateDesc: String, progress: (Int) -> Unit)


    /**
     * 获取APP应用市场状态
     * @param applicationId 包名
     */
    @Throws
    abstract suspend fun getMarketState(applicationId: String): MarketState

    /**
     * 声明需要的参数
     */
    data class Param(

        /** 参数名称，如api_key */
        val name: String,

        /** 默认参数 */
        val defaultValue: String? = null,

        /** 参数的描述，可为空 */
        val desc: String? = null,
        /**
         * 参数类型
         */
        val type: ParmaType = ParmaType.Text
    )

    /**
     * 参数类型
     */
    sealed class ParmaType {
        /**
         * 文本类型
         */
        data object Text : ParmaType()

        /**
         * 纯文字类型的文件
         * @param fileExtension 允许的文件扩展名
         */
        data class TextFile(val fileExtension: String) : ParmaType()
    }


    interface SubmitStateListener {

        fun onStart()

        fun onProcessing(action: String)

        /**
         * 取值范围0到100
         */
        fun onProgress(progress: Int)

        fun onSuccess()

        fun onError(exception: Exception)
    }

    companion object {
        const val FILE_NAME_IDENTIFY = "fileNameIdentify"

        private const val LOG_TAG = "渠道上传"
    }
}