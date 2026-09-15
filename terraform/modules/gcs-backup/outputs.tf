output "bucket_name" {
  description = "اسم حاوية النسخ الاحتياطي"
  value       = google_storage_bucket.backup.name
}

output "bucket_url" {
  description = "رابط GCS للحاوية"
  value       = google_storage_bucket.backup.url
}

output "service_account_email" {
  description = "بريد حساب الخدمة الخاص بالنسخ"
  value       = google_service_account.backup_sa.email
}
