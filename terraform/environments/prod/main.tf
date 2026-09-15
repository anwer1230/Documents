module "root" {
  source = "../../"

  project_id       = var.project_id
  primary_region   = var.primary_region
  secondary_region = var.secondary_region
  app_name         = var.app_name
  image_tag        = var.image_tag
  domain           = var.domain
}

variable "project_id" { type = string }
variable "primary_region" { type = string; default = "us-central1" }
variable "secondary_region" { type = string; default = "europe-west1" }
variable "app_name" { type = string; default = "telegram-web" }
variable "image_tag" { type = string; default = "latest" }
variable "domain" { type = string }

output "global_ip" { value = module.root.global_ip }
output "backup_bucket" { value = module.root.backup_bucket }
