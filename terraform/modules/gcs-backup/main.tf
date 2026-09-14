resource "google_storage_bucket" "backup" {
  name          = var.bucket_name
  project       = var.project_id
  location      = var.location
  force_destroy = false

  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = var.retention_days
    }
  }

  lifecycle_rule {
    action {
      type = "AbortIncompleteMultipartUpload"
    }
    condition {
      age = 1
    }
  }

  labels = {
    environment = var.environment
    service     = "telegram-web"
    component   = "backup"
  }
}

# حساب خدمة مخصص لوظائف النسخ الاحتياطي بأدنى صلاحيات
resource "google_service_account" "backup_sa" {
  account_id   = "telegram-backup-${var.environment}"
  display_name = "Telegram Backup Service Account"
  project      = var.project_id
}

resource "google_storage_bucket_iam_member" "backup_writer" {
  bucket = google_storage_bucket.backup.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.backup_sa.email}"
}
