output "global_ip" {
  description = "عنوان IP الذي يوجه إليه النطاق في DNS"
  value       = module.load_balancer.global_ip
}

output "backup_bucket" {
  description = "اسم حاوية النسخ الاحتياطية"
  value       = module.backup_storage.bucket_name
}

output "primary_url" {
  value = module.cloud_run_primary.service_url
}

output "secondary_url" {
  value = module.cloud_run_secondary.service_url
}
