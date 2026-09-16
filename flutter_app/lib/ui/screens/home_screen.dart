import 'package:flutter/material.dart';
import 'dashboard_screen.dart';
import 'insights_screen.dart';
import 'quran_hub_screen.dart';
import 'settings_screen.dart';
import 'qibla_screen.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';
import '../../providers/salah_provider.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

  @override
  void initState() {
    super.initState();
    _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    Map<Permission, PermissionStatus> statuses = await [
      Permission.locationWhenInUse,
      Permission.notification,
      Permission.scheduleExactAlarm,
    ].request();

    if (statuses[Permission.locationWhenInUse] == PermissionStatus.granted) {
      if (mounted) {
        Provider.of<SalahProvider>(context, listen: false).fetchCurrentLocation();
      }
    }
  }

  static const List<Widget> _widgetOptions = <Widget>[
    DashboardScreen(),
    QuranHubScreen(),
    InsightsScreen(),
    SettingsScreen(),
    QiblaScreen(),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: _widgetOptions.elementAt(_selectedIndex),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: _onItemTapped,
        backgroundColor: const Color(0xFF121212),
        indicatorColor: Colors.transparent,
        destinations: const <NavigationDestination>[
          NavigationDestination(
            icon: Icon(Icons.schedule, color: Colors.white54),
            selectedIcon: Icon(Icons.schedule, color: Color(0xFF3A9AD9)),
            label: 'Dashboard',
          ),
          NavigationDestination(
            icon: Icon(Icons.menu_book, color: Colors.white54),
            selectedIcon: Icon(Icons.menu_book, color: Color(0xFF3A9AD9)),
            label: 'Quran',
          ),
          NavigationDestination(
            icon: Icon(Icons.alarm, color: Colors.white54),
            selectedIcon: Icon(Icons.alarm, color: Color(0xFF3A9AD9)),
            label: 'Insights',
          ),
          NavigationDestination(
            icon: Icon(Icons.language, color: Colors.white54),
            selectedIcon: Icon(Icons.language, color: Color(0xFF3A9AD9)),
            label: 'Settings',
          ),
          NavigationDestination(
            icon: Icon(Icons.explore, color: Colors.white54),
            selectedIcon: Icon(Icons.explore, color: Color(0xFF3A9AD9)),
            label: 'Qibla',
          ),
        ],
      ),
    );
  }
}
