variable "project_id" {
  description = "معرف مشروع Google Cloud"
  type        = string
}

variable "primary_region" {
  description = "المنطقة الجغرافية الأساسية"
  type        = string
  default     = "us-central1"
}

variable "secondary_region" {
  description = "المنطقة الجغرافية الاحتياطية"
  type        = string
  default     = "europe-west1"
}

variable "app_name" {
  description = "اسم التطبيق"
  type        = string
  default     = "telegram-web"
}

variable "image_tag" {
  description = "وسم صورة Docker"
  type        = string
  default     = "latest"
}

variable "domain" {
  description = "النطاق المخصص للتطبيق"
  type        = string
  default     = "telegram.example.com"
}
