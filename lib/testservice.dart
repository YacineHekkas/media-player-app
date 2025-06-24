import 'package:flutter/services.dart';
import 'package:file_picker/file_picker.dart';

class AudioPlayerService {
  static const _channel = MethodChannel('audio_service');
  static Function(bool)? onPlaybackStateChanged;

  static Future<void> init() async {
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onPlaybackStateChanged':
          onPlaybackStateChanged?.call(call.arguments as bool);
          break;
      }
    });
    await _channel.invokeMethod('initService');
  }

  static Future<void> playFile(String filePath) async {
    await _channel.invokeMethod('play', {'path': filePath});
  }

  static Future<void> pause() async {
    await _channel.invokeMethod('pause');
  }

  static Future<String?> pickAudioFile() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.audio,
      allowCompression: false,
    );
    return result?.files.single.path;
  }
}