package reading520.com.uhf

import android.annotation.SuppressLint
import android.content.*
import android.device.ScanDevice
import io.flutter.plugin.common.EventChannel

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar
import reading520.com.uhf.options.ErrorCode
import reading520.com.uhf.options.UhfReaderManager
import reading520.com.uhf.options.UhfResult

class UhfPlugin private constructor(private val registrar: Registrar) : MethodCallHandler {
    private val uhfReaderManager = UhfReaderManager.getInstance()
    private val sharedPreferences: SharedPreferences by lazy { registrar.context().getSharedPreferences(UhfReaderManager.SHARE_PREFERENCES, Context.MODE_PRIVATE) }
    private var eventChannel:EventChannel?=null
    private var eventSink:EventChannel.EventSink?=null
    private var broadCastEventSink:EventChannel.EventSink?=null
    private var broadCastEvent:EventChannel?=null
    private val SCAN_ACTION = "scan.rcv.message"
    private val scanDevice: ScanDevice = ScanDevice()
    private val mScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val barocode = intent.getByteArrayExtra("barocode")
            val barocodelen = intent.getIntExtra("length", 0)
            val barcodeStr = String(barocode, 0, barocodelen)
            println("我接收到了"+barcodeStr)
            broadCastEventSink?.success(barcodeStr)

        }

    }
    init {
        scanDevice.outScanMode=0
        scanDevice.startScan()
        val filter = IntentFilter()
        filter.addAction(SCAN_ACTION)
        registrar.activity().registerReceiver(mScanReceiver, filter)
        println("我被注册了")
        eventChannel= EventChannel(registrar.messenger(),"flutter.io/uhf/uhf")
        eventChannel!!.setStreamHandler(object :EventChannel.StreamHandler{
            override fun onListen(any: Any?, sink: EventChannel.EventSink?) {
                eventSink=sink
            }

            override fun onCancel(any: Any?) {
                eventSink=null
            }

        })

        broadCastEvent= EventChannel(registrar.messenger(),"flutter.io/uhf/broad")
        broadCastEvent?.setStreamHandler(object : EventChannel.StreamHandler{
            override fun onListen(any: Any?, sink: EventChannel.EventSink?) {
                broadCastEventSink=sink
            }

            override fun onCancel(any: Any?) {
                broadCastEventSink=null
            }

        })

    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "uhf")
            channel.setMethodCallHandler(UhfPlugin(registrar))


            //   注册广播 接收烧苗数据

        }

    }

    /**
     * 是否使用uhf
     */
    @SuppressLint("PrivateApi")
    private fun uhfCanUse(): Boolean {
        var canUse = false
        //是否坚持过本机可用uhf读写功能
        if (!sharedPreferences.getBoolean("IS_TEST", false)) {
            val edit = sharedPreferences.edit()
            try {
                Class.forName("android.os.IScanService\$Stub")
                edit.putBoolean("CAN_SCAN", true)
                canUse = true
            } catch (e: Exception) {
                edit.putBoolean("CAN_SCAN", false)
            }
            edit.putBoolean("IS_TEST", true).apply()
        } else {
            canUse = sharedPreferences.getBoolean("CAN_SCAN", false)
        }
        return canUse
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        when {
            call.method == "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            call.method == "isSupport" ->{
                result.success(uhfCanUse())
            }
            call.method == "isPowerOpen" -> result.success(uhfReaderManager.isPowerOpen())
            call.method=="openUhf"->{
                val powerResult = UhfReaderManager.getInstance().psampoweron()
                when (powerResult) {
                    is UhfResult.Success -> result.success(powerResult.text)
                    is UhfResult.Fail -> result.error("-1", powerResult.code.text, null)
                }
            }
            call.method == "connectAndOpenUhf" -> {
                uhfReaderManager.password=call.argument("password")
                uhfReaderManager.password2=call.argument("password2")
                if (!uhfReaderManager.isConnect() && !uhfReaderManager.connect(registrar.context())) {
                    result.error("-1", ErrorCode.SERIAL_PORT_INIT_ERROR.text, null)
                    return
                }
                UhfReaderManager.getInstance().setOnScanListener(
                        object :UhfReaderManager.OnScanListener{
                            override fun onRecive(epc: String) {
                                eventSink?.success(epc)
                            }

                        }
                )
                val powerResult = UhfReaderManager.getInstance().psampoweron()

                when (powerResult) {
                    is UhfResult.Success ->{
                        result.success(powerResult.text)
                    }
                    is UhfResult.Fail -> result.error("-1", powerResult.code.text, null)
                }

            }
            call.method == "closeUhf" -> {
                uhfReaderManager.psampoweroff()
            }
            call.method == "stopScan" -> {
                uhfReaderManager.stopScanning()
            }
            call.method == "startScan" -> {
                result.success(uhfReaderManager.startScanning())
            }
            call.method=="changeLabelContent"->{
                val resultMessage =uhfReaderManager.changeEpcContent(call.argument("epc"),call.argument("newEpc"))
                when(resultMessage){
                    is UhfResult.Success->result.success(resultMessage.text)
                    is UhfResult.Fail->result.error("-1", resultMessage.code.text, null)
                }
            }
            call.method == "close" -> {
                uhfReaderManager.close()
                result.success(true)
            }
            call.method =="changeLabelLength"->{
              val resultMessage =uhfReaderManager.changeLabelLength(call.argument("epc"),call.argument("length"))
              when(resultMessage){
                  is UhfResult.Success->result.success(resultMessage.text)
                  is UhfResult.Fail->result.error("-1", resultMessage.code.text, null)
              }
            }
            call.method == "connect" -> {
                if (registrar.context()==null){
                    result.error("-1", "connect fail", null)
                    return
                }
                if (uhfReaderManager.connect(registrar.context())) {
                    result.success("connect success")
                } else {
                    result.error("-1", "connect fail", null)
                }
            }

            else -> result.notImplemented()
        }
    }
}


