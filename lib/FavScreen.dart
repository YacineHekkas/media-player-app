import 'package:flutter/material.dart';
import 'package:tp_mobile/songDetails.dart';

import 'data.dart';

class Favscreen extends StatefulWidget {
  const Favscreen({super.key});

  @override
  State<Favscreen> createState() => _FavscreenState();
}

class _FavscreenState extends State<Favscreen> {
  List<Song> favoriteSongs = [];
  Song? selectedSong;

  @override
  void initState() {
    super.initState();
    getFavorites();
  }

  Future<void> getFavorites() async {
    final favorites = await DatabaseHelper.instance.getFavorites();
    setState(() {
      favoriteSongs = favorites;
    });
  }

  Future<void> _removeFavorite(Song song) async {
    await DatabaseHelper.instance.deleteFavorite(song.id);
    setState(() {
      favoriteSongs.removeWhere((s) => s.id == song.id);
    });
  }



  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Favorite"),
      ),
      body: OrientationBuilder(
        builder: (context, orientation) {
          if (orientation == Orientation.portrait) {
            return SongsList(
              songs: favoriteSongs,
              onSongSelected: (song) {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => SongDetailScreen(song: song),
                  ),
                );
              }, deleteSong: (Song ) {
                _removeFavorite(Song);
            },
            );
          }
          else {
            return Row(
              children: [
                Expanded(
                  flex: 1,
                  child: SongsList(
                    songs: favoriteSongs,

                    onSongSelected: (song) {
                      setState(() {
                        selectedSong = song;
                      });
                    },
                    selectedSong: selectedSong,
                    deleteSong: (Song ) {
                    _removeFavorite(Song);
            },
                  ),
                ),
                Expanded(
                  flex: 2,
                  child: selectedSong != null
                      ? SingleChildScrollView(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          selectedSong!.title,
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 20),
                        const Text(
                          'Description:',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                            color: Colors.grey,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          selectedSong!.description,
                          style: const TextStyle(
                            fontSize: 16,
                            height: 1.5,
                          ),
                        ),
                      ],
                    ),
                  )
                      : const Center(
                    child: Text(
                      'Select a song ',
                      style: TextStyle(fontSize: 18, color: Colors.grey),
                    ),
                  ),
                ),
              ],
            );
          }
        },
      ),
    );
  }
}

class SongsList extends StatelessWidget {
  final List<Song> songs;
  final Function(Song) onSongSelected;
  final Function(Song) deleteSong;
  final Song? selectedSong;

  const SongsList({
    super.key,
    required this.songs,
    required this.onSongSelected,
    this.selectedSong,
    required this.deleteSong,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: songs.length,
      itemBuilder: (context, index) {
        final song = songs[index];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          elevation: 2,
          color: selectedSong?.id == song.id
              ? Theme.of(context).primaryColor.withOpacity(0.2)
              : null,
          child: ListTile(
            contentPadding: const EdgeInsets.all(16),
            title: Text(
              song.title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            subtitle: Text(
              song.description,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 14),
            ),
            onLongPress: ()=>deleteSong(song),
            onTap: () => onSongSelected(song),
          ),
        );
      },
    );
  }
}


