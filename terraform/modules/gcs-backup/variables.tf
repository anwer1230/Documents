variable "project_id" {
  description = "معرف مشروع GCP"
  type        = string
}

variable "bucket_name" {
  description = "اسم حاوية GCS للنسخ الاحتياطي"
  type        = string
}

variable "location" {
  description = "الموقع الجغرافي لحاوية GCS"
  type        = string
  default     = "US"
}

variable "retention_days" {
  description = "عدد أيام الاحتفاظ بالنسخ الاحتياطية"
  type        = number
  default     = 30
}

variable "environment" {
  description = "بيئة التشغيل (prod/staging)"
  type        = string
}
