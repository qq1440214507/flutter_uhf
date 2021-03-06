import 'dart:async';

import 'package:flutter/services.dart';
import 'package:uhf/uhf_result.dart';

class Uhf {
  static const MethodChannel _channel = const MethodChannel('uhf');

  static Future<UhfResult> invoke(String method) async {
    UhfResult result;
    try {
      final text = await _channel.invokeMethod(method);
      result = UhfResult.createSuccess(text);
    } on PlatformException catch (e) {
      result = UhfResult.createFail(e);
    }
    return result;
  }

  /// 获取平台号
  static Future<String> get platformVersion async {
    final result = await invoke("getPlatformVersion");
    return result.message;
  }

  //销毁
  static Future<bool> get destroyed async {
    return await _channel.invokeMethod("close");
  }

  //开启电源
  static Future<bool> get openUhf async {
    UhfResult isSuccess = await _channel.invokeMethod("openUhf");
    return isSuccess.isSuccess;
  }

  //开启电源
  static Future<UhfResult> get connectAndOpenUhf async {
    UhfResult result;
    print("开始连接");
    try {
      final text = await _channel.invokeMethod("connectAndOpenUhf",
      <String,String>{
        "password":"00000000",// a password for epc，if only  one use password
        "password2":"61994087",//a password for epc，if you have anther password add this params,if more you can change resource to array
      });
      print("没有异常");
      result = UhfResult.createSuccess(text);
      print("获得结果");
    } on PlatformException catch (e) {
      print("异常了");
      print(e);
      result = UhfResult.createFail(e);
    }
    return result;
  }

  //初始化串口
  static Future<bool> get connect async {
    print("开始连接电源");
    UhfResult result = await invoke("connect");
    print("连接电源完毕"+result.message);
    return result.isSuccess;
  }

  //开始扫描
  static Future<bool> get startScan async {
    return await _channel.invokeMethod("startScan");
  }
  //开始扫描
  static void get stopScan async {
       _channel.invokeMethod("stopScan");
  }

  //关闭电源
  static Future<void> get closeUhf async {
    return await _channel.invokeMethod('closeUhf');
  }

  //是否支持
  static Future<bool> get supportUhf async {
    return await _channel.invokeMethod('isSupport');
  }

  //是否支持
  static Future<bool> get isPowerOpen async {
    return await _channel.invokeMethod('isPowerOpen');
  }

  //修改电子标签长度
  // length 1000 -> 8
  static Future<String> changeLabelLength(String epc, String length) async {
    return await _channel.invokeMethod(
      'changeLabelLength',
      <String, String>{'epc': epc,'length':length},
    );
  }

  // change new epc
  static Future<String> changeLabelContent(String epc, String newEpc) async {
    return await _channel.invokeMethod(
      'changeLabelContent',
      <String, String>{'epc': epc,'newEpc':newEpc},
    );
  }
}
