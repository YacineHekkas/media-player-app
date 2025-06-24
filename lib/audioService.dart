import 'package:flutter/services.dart';

class AudioService {
  static const MethodChannel _channel = MethodChannel('com.example.tp_mobile/audio');

  // Callback for playback state changes
  Function(bool isPlaying, String songName, String artistName)? onPlaybackStateChanged;

  AudioService() {
    _channel.setMethodCallHandler(_handleMethodCall);

    // Register for broadcast events
    _channel.invokeMethod('registerBroadcastReceiver');
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onPlaybackStateChanged':
        if (onPlaybackStateChanged != null) {
          final Map<dynamic, dynamic> data = call.arguments as Map<dynamic, dynamic>;
          final bool isPlaying = data['isPlaying'] as bool;
          final String songName = data['songName'] as String;
          final String artistName = data['artistName'] as String;

          onPlaybackStateChanged!(isPlaying, songName, artistName);
        }
        break;
      default:
        print('Method not implemented: ${call.method}');
    }
  }

  Future<void> playAudio({String? filePath, String? songName, String? artistName}) async {
    try {
      final Map<String, dynamic> arguments = {
        'filePath': filePath,
        'songName': songName ?? 'Unknown Track',
        'artistName': artistName ?? 'Unknown Artist',
      };

      await _channel.invokeMethod('playAudio', arguments);
    } on PlatformException catch (e) {
      print('Error playing audio: ${e.message}');
    } catch (e) {
      print('Unexpected error: $e');
    }
  }

  Future<void> pauseAudio() async {
    try {
      await _channel.invokeMethod('pauseAudio');
    } on PlatformException catch (e) {
      print('Error pausing audio: ${e.message}');
    }
  }

  void dispose() {
    // Clean up resources
    _channel.invokeMethod('unregisterBroadcastReceiver');
  }
}
