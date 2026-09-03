# Mess Management App

An Android application designed to digitize and automate the daily meal tracking, shared expense management, and monthly financial settlement for bachelor messes, hostels, and shared living arrangements.

---

## Overview

In common living situations like bachelor messes, managing daily meals and shared costs is typically done manually — leading to calculation errors, missing records, and disputes among members. This app provides a centralized, role-based Android solution where every member can transparently track meals and expenses, while the Admin maintains full control over pricing and member management.

---

## Features

### Admin
- **Member Management** — View, approve, and manage all registered members.
- **Set Meal Prices** — Configure per-meal costs (Breakfast, Lunch, Dinner) that apply to all members.
- **Expense Management** — Log and track shared expenses across the mess.
- **Monthly Summary Report** — Generate and view end-of-month financial summaries and dues.
- **Balance & Status Overview** — Real-time visibility into total meals consumed, total expenses, and payment balances.

### Members
- **Daily Meal Entry** — Log breakfast, lunch, and dinner participation each day.
- **Expense Addition** — Add personal or shared expenses beyond meals.
- **Real-Time Balance** — View current dues, paid amount, and outstanding balance at any time.
- **Payment** — Record payments directly through the app.
- **Profile Management** — Update personal details and sync with cloud.

### System-Wide
- **Role-Based Access Control** — Strict separation between Admin and Member permissions.
- **Offline Support** — SQLite stores data locally so the app works without internet.
- **Cloud Synchronization** — Firebase keeps data backed up and synced across devices.
- **Push Notifications** — Alerts for meal reminders, expense additions, and payment confirmations.
- **Automated Calculation Engine** — Automatically computes each member's meal cost, total dues, and balance based on configured prices.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| IDE | Android Studio |
| UI | XML Layouts, Activities & Intents |
| Local Database | SQLite |
| Cloud Database | Firebase Realtime Database |
| Authentication | Firebase Authentication |
| Notifications | Firebase Cloud Messaging (FCM) |
| Min SDK | Android 7.0 (API 24) |

---

## Architecture

The app follows a structured data flow:

1. **User Authentication** — Login/Registration with role verification (Admin or Member).
2. **Dashboard Routing** — Users are directed to their role-specific dashboard.
3. **Data Entry** — Meal and expense entries are saved first to **SQLite** (offline-first).
4. **Cloud Sync** — Data is synchronized to **Firebase** in the background when online.
5. **Calculation Engine** — Automatically computes meal rates and dues per member.
6. **Monthly Report** — Aggregated summary generated for settlement at month-end.

---

## Key Screens

| Screen | Description |
|--------|-------------|
| Sign-Up Page | New users register with personal info and select a role (Admin / Member) |
| Login Page | Secure authentication with remember-me functionality |
| Admin Dashboard | Overview of total meals, expenses, balance status, and meal price breakdown |
| Member Dashboard | Personal meal entry, balance view, and payment options |
| Set Meal Prices | Admin sets per-meal costs for Breakfast, Lunch, and Dinner |
| Add Expense | Log additional shared expenses with details |
| Member List | Admin can view all members and approve new registrations |
| Monthly Summary | End-of-month financial report for all members |
| Profile & Cloud Sync | Update profile details and trigger cloud synchronization |

---

## Getting Started

### Prerequisites
- Android Studio (latest stable version)
- Android device or emulator running Android 7.0 (API 24) or higher
- A Firebase project with Realtime Database and Authentication enabled

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/mess-management-app.git
   ```
2. Open the project in **Android Studio**.
3. Connect your Firebase project:
   - Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
   - Download the `google-services.json` file and place it in the `app/` directory.
4. Enable **Firebase Authentication** (Email/Password) and **Realtime Database** in the Firebase Console.
5. Build and run the project on your device or emulator.

---

##  Usage

**As an Admin:**
1. Register with the Admin role and log in.
2. Set meal prices for Breakfast, Lunch, and Dinner.
3. Approve new member registrations from the Members screen.
4. View the monthly summary to track dues and settle accounts.

**As a Member:**
1. Register with the Member role and wait for Admin approval.
2. Log your daily meal participation (Breakfast / Lunch / Dinner).
3. Add any shared expenses when applicable.
4. Check your real-time balance and record payments as needed.

---

##  Future Enhancements

- In-app payment gateway integration (bKash, Nagad, etc.)
- Meal voting / planning system
- SMS/email notifications for monthly dues
- Data export to PDF/Excel for financial records
- iOS version

---

## 📄 License

This project was developed as an academic project for the course **CSE489 – Mobile Programming** at East West University. It is intended for educational purposes.
