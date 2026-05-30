# KosHub AlwaysData Backend

This directory contains PHP scripts to handle KosHub payments as a proxy to the AlwaysData payment gateway.
This replaces the Firebase Cloud Functions previously used.

## Deployment

1. Copy all files in this directory to your AlwaysData PHP hosting.
2. Generate a Service Account JSON from Firebase Console.
3. Rename it to `service-account.json` and upload it here.
4. Edit `config.php` to set your `FIREBASE_PROJECT_ID` and `PAYMENT_API_KEY`.
5. Test connectivity by visiting `test_firestore.php` in your browser.

## Cron Job

You can set up a Cron job on AlwaysData to run `koshub_expire_payments.php` every 5-10 minutes:
`php /home/username/www/koshub_expire_payments.php`
