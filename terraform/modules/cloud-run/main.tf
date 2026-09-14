resource "google_cloud_run_service" "app" {
  name     = var.service_name
  location = var.region
  project  = var.project_id

  template {
    spec {
      containers {
        image = var.image

        resources {
          limits = {
            cpu    = "2000m"
            memory = "1024Mi"
          }
        }

        dynamic "env" {
          for_each = var.env_vars
          content {
            name  = env.key
            value = env.value
          }
        }

        ports {
          container_port = 3000
        }

        startup_probe {
          http_get {
            path = "/api/health"
            port = 3000
          }
          initial_delay_seconds = 5
          period_seconds        = 10
          failure_threshold     = 3
        }

        liveness_probe {
          http_get {
            path = "/api/health"
            port = 3000
          }
          period_seconds    = 15
          failure_threshold = 3
        }
      }
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/minScale" = tostring(var.min_instances)
        "autoscaling.knative.dev/maxScale" = tostring(var.max_instances)
      }
      labels = {
        environment = var.environment
        region      = var.region
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

# السماح بالوصول العام للخدمة
resource "google_cloud_run_service_iam_member" "public_access" {
  service  = google_cloud_run_service.app.name
  location = google_cloud_run_service.app.location
  project  = var.project_id
  role     = "roles/run.invoker"
  member   = "allUsers"
}
