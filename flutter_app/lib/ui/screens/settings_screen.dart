import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/salah_provider.dart';
import '../../providers/auth_provider.dart';
import '../../data/calculator/prayer_time_calculator.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Settings', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Consumer<SalahProvider>(
        builder: (context, provider, child) {
          final prefs = provider.userPreferences;

          return ListView(
            padding: const EdgeInsets.all(16.0),
            children: [
              _buildSectionTitle('Account'),
              Consumer<AuthProvider>(
                builder: (context, authProvider, child) {
                  final user = authProvider.currentUser;
                  return Card(
                    color: const Color(0xFF1E1E1E),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        children: [
                          if (user != null) ...[
                            CircleAvatar(
                              backgroundImage: user.photoURL != null ? NetworkImage(user.photoURL!) : null,
                              child: user.photoURL == null ? const Icon(Icons.person) : null,
                            ),
                            const SizedBox(height: 8),
                            Text(user.displayName ?? 'User', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                            Text(user.email ?? '', style: const TextStyle(color: Colors.white54)),
                            const SizedBox(height: 16),
                            ElevatedButton(
                              onPressed: () => authProvider.signOut(),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.redAccent,
                                foregroundColor: Colors.white,
                                minimumSize: const Size.fromHeight(48),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                              child: const Text('Sign Out'),
                            ),
                          ] else ...[
                            const Text('Sign in to sync your data across devices.', style: TextStyle(color: Colors.white54), textAlign: TextAlign.center),
                            const SizedBox(height: 16),
                            ElevatedButton.icon(
                              onPressed: authProvider.isLoading ? null : () async {
                                try {
                                  await authProvider.signInWithGoogle();
                                } catch (e) {
                                  if (context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text("Sign-in failed. Please ensure Play Console SHA-1 is added to Firebase.")),
                                    );
                                  }
                                }
                              },
                              icon: authProvider.isLoading 
                                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                  : const Icon(Icons.login),
                              label: const Text('Sign In with Google'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white,
                                foregroundColor: Colors.black87,
                                minimumSize: const Size.fromHeight(48),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  );
                },
              ),
              const SizedBox(height: 24),
              _buildSectionTitle('Location Settings'),
              Card(
                color: const Color(0xFF1E1E1E),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Text(
                        prefs.selectedCity.isEmpty ? "Location: Unknown" : "Location: ${prefs.selectedCity}",
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        "Coordinates: ${prefs.latitude.toStringAsFixed(4)}, ${prefs.longitude.toStringAsFixed(4)}",
                        style: const TextStyle(color: Colors.white54, fontSize: 12),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton.icon(
                        onPressed: () {
                          // Auto detect location
                        },
                        icon: const Icon(Icons.my_location),
                        label: const Text("Auto-Detect Location"),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF3A9AD9),
                          foregroundColor: Colors.white,
                          minimumSize: const Size.fromHeight(48),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              _buildSectionTitle('Calculation Configuration'),
              Card(
                color: const Color(0xFF1E1E1E),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text("Calculation Method", style: TextStyle(color: Colors.white54, fontSize: 12)),
                      const SizedBox(height: 8),
                      DropdownButtonFormField<String>(
                        dropdownColor: const Color(0xFF1E1E1E),
                        decoration: InputDecoration(
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        ),
                        value: prefs.calculationMethod,
                        style: const TextStyle(color: Colors.white),
                        items: CalculationMethod.values.map((method) {
                          return DropdownMenuItem(
                            value: method.name.toUpperCase(),
                            child: Text(method.name.toUpperCase()),
                          );
                        }).toList(),
                        onChanged: (value) {},
                      ),
                      const SizedBox(height: 16),
                      const Text("Asr Juristic Method", style: TextStyle(color: Colors.white54, fontSize: 12)),
                      const SizedBox(height: 8),
                      DropdownButtonFormField<String>(
                        dropdownColor: const Color(0xFF1E1E1E),
                        decoration: InputDecoration(
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        ),
                        value: prefs.juristicMethod,
                        style: const TextStyle(color: Colors.white),
                        items: JuristicMethod.values.map((method) {
                          return DropdownMenuItem(
                            value: method.name.toUpperCase(),
                            child: Text(method.name.toUpperCase()),
                          );
                        }).toList(),
                        onChanged: (value) {},
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              _buildSectionTitle('Notifications & Reminders'),
              Card(
                color: const Color(0xFF1E1E1E),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: [
                    _buildSwitch("Fajr Notification", prefs.fajrNotifEnabled, (v) {}),
                    _buildSwitch("Dhuhr Notification", prefs.dhuhrNotifEnabled, (v) {}),
                    _buildSwitch("Asr Notification", prefs.asrNotifEnabled, (v) {}),
                    _buildSwitch("Maghrib Notification", prefs.maghribNotifEnabled, (v) {}),
                    _buildSwitch("Isha Notification", prefs.ishaNotifEnabled, (v) {}),
                    const Divider(color: Colors.white12),
                    _buildSwitch("Missed Prayer Qaza Reminders", prefs.missedPrayerRemindersEnabled, (v) {}),
                    _buildSwitch("Post-Salah Recitation Prompt", prefs.postSalahRecitationEnabled, (v) {}),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: Text(
        title,
        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF3A9AD9)),
      ),
    );
  }

  Widget _buildSwitch(String title, bool value, Function(bool) onChanged) {
    return SwitchListTile(
      title: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500)),
      value: value,
      onChanged: onChanged,
      activeColor: Colors.white,
      activeTrackColor: const Color(0xFF3A9AD9),
    );
  }
}
