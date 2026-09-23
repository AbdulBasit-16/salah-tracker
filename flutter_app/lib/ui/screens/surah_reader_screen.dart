import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import '../../providers/quran_provider.dart';
import '../../data/models/surah_data.dart';

class SurahReaderScreen extends StatefulWidget {
  final Surah surah;

  const SurahReaderScreen({super.key, required this.surah});

  @override
  State<SurahReaderScreen> createState() => _SurahReaderScreenState();
}

class _SurahReaderScreenState extends State<SurahReaderScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final provider = Provider.of<QuranProvider>(context, listen: false);
      provider.loadSurahVerses(
        context,
        widget.surah.number,
        provider.preferences.quranScript,
        provider.preferences.showEnglishTranslation,
        provider.preferences.showUrduTranslation,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: Text(widget.surah.nameEnglish, style: const TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF121212),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              // Show settings dialog
              _showSettingsDialog(context);
            },
          ),
        ],
      ),
      body: Consumer<QuranProvider>(
        builder: (context, provider, child) {
          if (provider.isLoadingSurah) {
            return const Center(child: CircularProgressIndicator(color: Color(0xFF3A9AD9)));
          }

          if (provider.activeSurahVerses.isEmpty) {
            return const Center(child: Text("Error loading Surah", style: TextStyle(color: Colors.red)));
          }

          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: provider.activeSurahVerses.length,
            separatorBuilder: (context, index) => const Divider(color: Colors.white12, height: 32),
            itemBuilder: (context, index) {
              final verse = provider.activeSurahVerses[index];
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Verse number badge
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF3A9AD9).withOpacity(0.15),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        "${verse.chapter}:${verse.verse}",
                        style: const TextStyle(color: Color(0xFF3A9AD9), fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  
                  // Arabic Text
                  Text(
                    verse.arabicText,
                    textAlign: TextAlign.right,
                    textDirection: TextDirection.rtl,
                    style: TextStyle(
                      fontSize: 28,
                      height: 1.8,
                      color: Colors.white,
                      fontFamily: provider.preferences.quranScript == "INDOPAK" ? 'NooreHuda' : 'Uthmani',
                    ),
                  ),
                  
                  // Urdu Translation
                  if (provider.preferences.showUrduTranslation && verse.urduText != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      verse.urduText!,
                      textAlign: TextAlign.right,
                      textDirection: TextDirection.rtl,
                      style: const TextStyle(
                        fontSize: 20,
                        height: 1.5,
                        color: Colors.white70,
                        fontFamily: 'JameelNooriNastaleeq',
                      ),
                    ),
                  ],

                  // English Translation
                  if (provider.preferences.showEnglishTranslation && verse.englishText != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      verse.englishText!,
                      textAlign: TextAlign.left,
                      style: const TextStyle(
                        fontSize: 16,
                        height: 1.5,
                        color: Colors.white60,
                      ),
                    ),
                  ],
                ],
              );
            },
          );
        },
      ),
    );
  }

  void _showSettingsDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text("Reading Settings", style: TextStyle(color: Colors.white)),
          content: Consumer<QuranProvider>(
            builder: (context, provider, child) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<String>(
                    dropdownColor: const Color(0xFF1E1E1E),
                    decoration: const InputDecoration(labelText: 'Arabic Script', labelStyle: TextStyle(color: Colors.white54)),
                    value: provider.preferences.quranScript,
                    style: const TextStyle(color: Colors.white),
                    items: const [
                      DropdownMenuItem(value: "UTHMANI", child: Text("Uthmani Script")),
                      DropdownMenuItem(value: "INDOPAK", child: Text("Indo-Pak Script")),
                    ],
                    onChanged: (val) {
                      if (val != null) {
                        provider.updateQuranPreferences(val, provider.preferences.showEnglishTranslation, provider.preferences.showUrduTranslation);
                        provider.loadSurahVerses(context, widget.surah.number, val, provider.preferences.showEnglishTranslation, provider.preferences.showUrduTranslation);
                      }
                    },
                  ),
                  const SizedBox(height: 16),
                  SwitchListTile(
                    title: const Text("English Translation", style: TextStyle(color: Colors.white)),
                    value: provider.preferences.showEnglishTranslation,
                    activeColor: const Color(0xFF3A9AD9),
                    onChanged: (val) {
                      provider.updateQuranPreferences(provider.preferences.quranScript, val, provider.preferences.showUrduTranslation);
                      provider.loadSurahVerses(context, widget.surah.number, provider.preferences.quranScript, val, provider.preferences.showUrduTranslation);
                    },
                  ),
                  SwitchListTile(
                    title: const Text("Urdu Translation", style: TextStyle(color: Colors.white)),
                    value: provider.preferences.showUrduTranslation,
                    activeColor: const Color(0xFF3A9AD9),
                    onChanged: (val) {
                      provider.updateQuranPreferences(provider.preferences.quranScript, provider.preferences.showEnglishTranslation, val);
                      provider.loadSurahVerses(context, widget.surah.number, provider.preferences.quranScript, provider.preferences.showEnglishTranslation, val);
                    },
                  ),
                ],
              );
            },
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Close", style: TextStyle(color: Color(0xFF3A9AD9))),
            ),
          ],
        );
      },
    );
  }
}
