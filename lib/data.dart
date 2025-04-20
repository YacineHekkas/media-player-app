import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as p;

class Song {
  final String id;
  final String title;
  final String description;

  Song({
    required this.id,
    required this.title,
    required this.description,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'description': description,
    };
  }

  factory Song.fromMap(Map<String, dynamic> map) {
    return Song(
      id: map['id'],
      title: map['title'],
      description: map['description'],
    );
  }
}

// Dummy data
final List<Song> dummySongs = [
  Song(
    id: '1',
    title: 'Bohemian Rhapsody',
    description: 'A six-minute suite by the British rock band Queen, written by Freddie Mercury. It\'s a rock opera song and has no chorus but consists of several sections: an intro, a ballad segment, an operatic passage, a hard rock part and a reflective coda.',
  ),
  Song(
    id: '2',
    title: 'Stairway to Heaven',
    description: 'A song by the English rock band Led Zeppelin, released in 1971. It was composed by guitarist Jimmy Page and vocalist Robert Plant. It\'s often referred to as one of the greatest rock songs of all time.',
  ),
  Song(
    id: '3',
    title: 'Imagine',
    description: 'A song by English rock musician John Lennon from his 1971 album of the same name. The best-selling single of his solo career, its lyrics encourage listeners to imagine a world of peace without borders, possessions, or religion.',
  ),
  Song(
    id: '4',
    title: 'Smells Like Teen Spirit',
    description: 'A song by American rock band Nirvana, released in 1991. Written by Kurt Cobain, it\'s often credited with bringing alternative rock to mainstream popularity and is considered a cultural landmark.',
  ),
  Song(
    id: '5',
    title: 'Billie Jean',
    description: 'A song by American singer Michael Jackson, released in 1983. It was produced by Quincy Jones and written by Jackson. The song is about a woman who claims Jackson is the father of her child, which he denies.',
  ),
  Song(
    id: '6',
    title: 'Hotel California',
    description: 'A song by the American rock band Eagles, released in 1977. It\'s known for its extended guitar solo and cryptic lyrics that have been interpreted in various ways, often relating to themes of hedonism and excess in America.',
  ),
  Song(
    id: '7',
    title: 'Sweet Child O\' Mine',
    description: 'A song by American rock band Guns N\' Roses, released in 1988. Written by the band members, it features one of the most recognizable guitar riffs in rock music, played by Slash.',
  ),
  Song(
    id: '8',
    title: 'Like a Rolling Stone',
    description: 'A song by American singer-songwriter Bob Dylan, released in 1965. It\'s considered revolutionary in its combination of musical elements and confrontational lyrics, and is often ranked as one of the greatest songs of all time.',
  ),
];

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('songs.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = p.join(dbPath, filePath);

    return await openDatabase(path, version: 1, onCreate: _createDB);
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE favorites(
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT NOT NULL
      )
    ''');
  }

  Future<void> insertFavorite(Song song) async {
    final db = await database;
    await db.insert(
      'favorites',
      song.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> deleteFavorite(String id) async {
    final db = await database;
    await db.delete(
      'favorites',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<bool> isFavorite(String id) async {
    final db = await database;
    final result = await db.query(
      'favorites',
      where: 'id = ?',
      whereArgs: [id],
    );
    return result.isNotEmpty;
  }

  Future<List<Song>> getFavorites() async {
    final db = await database;
    final maps = await db.query('favorites');
    return List.generate(maps.length, (i) {
      return Song.fromMap(maps[i]);
    });
  }
}