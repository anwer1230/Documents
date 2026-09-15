terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
}

# 1. تخزين النسخ الاحتياطية
module "backup_storage" {
  source = "./modules/gcs-backup"

  project_id     = var.project_id
  bucket_name    = "${var.app_name}-backups-${var.project_id}"
  location       = "US"
  retention_days = 30
  environment    = "prod"
}

# 2. الخدمة في المنطقة الأساسية
module "cloud_run_primary" {
  source = "./modules/cloud-run"

  project_id    = var.project_id
  service_name  = "${var.app_name}-${var.primary_region}"
  region        = var.primary_region
  image         = "gcr.io/${var.project_id}/${var.app_name}:${var.image_tag}"
  min_instances = 1
  max_instances = 10
  environment   = "prod"
  env_vars = {
    NODE_ENV   = "production"
    GCS_BUCKET = module.backup_storage.bucket_name
  }
}

# 3. الخدمة في المنطقة الاحتياطية (Standby)
module "cloud_run_secondary" {
  source = "./modules/cloud-run"

  project_id    = var.project_id
  service_name  = "${var.app_name}-${var.secondary_region}"
  region        = var.secondary_region
  image         = "gcr.io/${var.project_id}/${var.app_name}:${var.image_tag}"
  min_instances = 0 # توفير التكلفة حتى وقت الطوارئ
  max_instances = 10
  environment   = "prod"
  env_vars = {
    NODE_ENV   = "production"
    GCS_BUCKET = module.backup_storage.bucket_name
  }
}

# 4. موزع الأحمال العالمي
module "load_balancer" {
  source = "./modules/load-balancer"

  project_id        = var.project_id
  name              = var.app_name
  primary_service   = module.cloud_run_primary.service_name
  primary_region    = var.primary_region
  secondary_service = module.cloud_run_secondary.service_name
  secondary_region  = var.secondary_region
  domain            = var.domain
}
