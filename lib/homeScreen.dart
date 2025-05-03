import 'dart:async';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:tp_mobile/FavScreen.dart';

import 'audioService.dart';
import 'data.dart';
import 'main.dart';

class MusicPlayer extends StatefulWidget {
  const MusicPlayer({super.key});

  @override
  State<MusicPlayer> createState() => _MusicPlayerState();
}

class _MusicPlayerState extends State<MusicPlayer>
    with RouteAware, WidgetsBindingObserver {

  final AudioService _audioService = AudioService();


  Song? selectedSong;
  int currentSongIndex = 0;
  late Timer _timer;
  bool isExpanded = false;
  bool isPaused = false;
  bool isFavorite = false;
  bool screenoff = false;

  final AudioPlayer _audioPlayer = AudioPlayer();

  @override
  void initState() {
    super.initState();
    _audioPlayer.setSource(AssetSource('audio/test.mp3'));

    _audioService.onPlaybackStateChanged = _handlePlaybackStateChanged;
    WidgetsBinding.instance.addObserver(this);

    selectedSong = dummySongs[currentSongIndex];
    _checkIfFavorite();

    // Set up timer to cycle through songs every 2 seconds
    _timer = Timer.periodic(const Duration(seconds: 2), (timer) {
      setState(() {
        currentSongIndex = (currentSongIndex + 1) % dummySongs.length;
        selectedSong = dummySongs[currentSongIndex];
        _checkIfFavorite();
      });
    });
  }

  void _handlePlaybackStateChanged(bool isPlaying) {
    setState(() {
      isPaused = !isPlaying;
    });
  }

  Future<void> _checkIfFavorite() async {
    if (selectedSong != null) {
      final isFav = await DatabaseHelper.instance.isFavorite(selectedSong!.id);
      setState(() {
        isFavorite = isFav;
      });
    }
  }

  @override
  void dispose() {
    routeObserver.unsubscribe(this);
    WidgetsBinding.instance.removeObserver(this);
    _audioPlayer.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    routeObserver.subscribe(this, ModalRoute.of(context)!);
  }

  Future<void> _playMusic() async {
    // await _audioPlayer.resume();


      _audioService.playAudio();
  }

  Future<void> _pauseMusic() async {
    // await _audioPlayer.pause();

      _audioService.pauseAudio();

  }

  @override
  void didPushNext() {
    setState(() {
      screenoff = false;
    });
    _pauseMusic();
  }

  @override
  void didPopNext() {
    if (!isPaused) {
      setState(() {
        screenoff = true;
      });
      _playMusic();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // if (state == AppLifecycleState.paused) {
    //   _pauseMusic();
    // }
    // else if (state == AppLifecycleState.resumed && !isPaused) {
    //   if (screenoff) {
    //     _playMusic();
    //   }
    // }
  }

  Future<void> addFavorite() async {
    if (selectedSong != null) {
      setState(() {
        isFavorite = !isFavorite;
      });

      if (isFavorite) {
        await DatabaseHelper.instance.insertFavorite(selectedSong!);
      } else {
        await DatabaseHelper.instance.deleteFavorite(selectedSong!.id);
      }
    }


  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              SizedBox(height: MediaQuery.of(context).size.height * 0.2),
              Center(
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 50),
                  transform: Matrix4.rotationZ(isPaused ? 0.2 : 0.0),
                  margin: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.grey.withOpacity(0.5),
                        spreadRadius: 2,
                        blurRadius: 5,
                        offset: const Offset(0, 3),
                      ),
                    ],
                  ),
                  clipBehavior: Clip.hardEdge,
                  child: Image.asset(
                    "assets/images/img.jpg",
                    fit: BoxFit.cover,
                    width: 280,
                    height: 200,
                  ),
                ),
              ),
              const SizedBox(height: 60),
              if (!isExpanded)
                IconButton(
                  icon: const Icon(Icons.play_arrow),
                  iconSize: 60,
                  onPressed: () {
                    setState(() {
                      isExpanded = true;
                    });
                  },
                )
              else
                Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.skip_previous),
                          iconSize: 60,
                          onPressed: () {
                          },
                        ),
                        if (!isPaused)
                          IconButton(
                            icon: const Icon(Icons.pause),
                            iconSize: 60,
                            onPressed: () {
                              setState(() {
                                isPaused = true;
                              });
                              _pauseMusic();
                            },
                          )
                        else
                          IconButton(
                            icon: const Icon(Icons.play_arrow),
                            iconSize: 60,
                            onPressed: () {
                              setState(() {
                                isPaused = false;
                              });
                              _playMusic();
                            },
                          ),
                        IconButton(
                          icon: const Icon(Icons.skip_next),
                          iconSize: 60,
                          onPressed: () {
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        GestureDetector(
                          onLongPress: () {
                            Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (context) => Favscreen(),
                              ),
                            );
                          },
                          onTap: addFavorite,
                          child: Icon(
                            isFavorite ? Icons.favorite : Icons.favorite_border,
                            color: isFavorite ? Colors.red : Colors.black,
                            size: 28,
                          ),
                        ),
                        Text(
                          selectedSong?.title ?? '',
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w400,
                          ),
                          textAlign: TextAlign.center,
                        ),

                      ],
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}

