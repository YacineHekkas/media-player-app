import 'package:flutter/material.dart';

class MusicPlayer extends StatefulWidget {
  const MusicPlayer({super.key});

  @override
  State<MusicPlayer> createState() => _MusicPlayerState();
}

class _MusicPlayerState extends State<MusicPlayer> {
  bool isExpanded = false;
  bool isPaused = false;
  bool isFavorite = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
               SizedBox(height: MediaQuery.of(context).size.height*0.2),
              // Container(
              //   margin: EdgeInsets.all(10),
              //   decoration: BoxDecoration(
              //     borderRadius: BorderRadius.circular(20),
              //     boxShadow: [
              //       BoxShadow(
              //         color: Colors.grey.withOpacity(0.2),
              //         spreadRadius: 2,
              //         blurRadius: 5,
              //         offset: Offset(0, 3),
              //       ),
              //     ],
              //   ),
              //   clipBehavior: Clip.hardEdge,
              //   child: Image.asset(
              //     "assets/images/img.jpg",
              //     fit: BoxFit.cover,
              //   ),
              // ),

              Center(
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 50),
                  transform: Matrix4.rotationZ(isPaused?0.2:0.0),
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
                          onPressed: () {},
                        ),
                        if (!isPaused)
                          IconButton(
                            icon: const Icon(Icons.pause),
                            iconSize: 60,
                            onPressed: () {
                              setState(() {
                                isPaused = true;
                              });
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
                            },
                          ),
                        IconButton(
                          icon: const Icon(Icons.skip_next),
                          iconSize: 60,
                          onPressed: () {},
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        IconButton(
                          icon: Icon(
                            isFavorite ? Icons.favorite : Icons.favorite_border,
                            color: isFavorite ? Colors.red : Colors.black,
                          ),
                          onPressed: () {
                            setState(() {
                              isFavorite = !isFavorite;
                            });
                          },
                        ),
                        const Text(
                          "go for some music",
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.black54,
                          ),
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
