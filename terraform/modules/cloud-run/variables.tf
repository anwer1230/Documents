variable "project_id" {
  description = "معرف مشروع GCP"
  type        = string
}

variable "service_name" {
  description = "اسم خدمة Cloud Run"
  type        = string
}

variable "region" {
  description = "منطقة النشر"
  type        = string
}

variable "image" {
  description = "رابط صورة Docker"
  type        = string
}

variable "min_instances" {
  description = "الحد الأدنى لعدد الحاويات"
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "الحد الأقصى لعدد الحاويات"
  type        = number
  default     = 10
}

variable "environment" {
  description = "البيئة (prod/staging)"
  type        = string
}

variable "env_vars" {
  description = "متغيرات البيئة للخدمة"
  type        = map(string)
  default     = {}
}
