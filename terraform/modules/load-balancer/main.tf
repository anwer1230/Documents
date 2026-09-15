# عنوان IP ثابت عالمي
resource "google_compute_global_address" "default" {
  name    = "${var.name}-ip"
  project = var.project_id
}

# شهادة SSL مُدارة تلقائياً
resource "google_compute_managed_ssl_certificate" "default" {
  name    = "${var.name}-cert"
  project = var.project_id

  managed {
    domains = [var.domain]
  }
}

# Network Endpoint Groups لـ Cloud Run
resource "google_compute_region_network_endpoint_group" "primary" {
  name                  = "${var.name}-primary-neg"
  project               = var.project_id
  region                = var.primary_region
  network_endpoint_type = "SERVERLESS"
  cloud_run {
    service = var.primary_service
  }
}

resource "google_compute_region_network_endpoint_group" "secondary" {
  name                  = "${var.name}-secondary-neg"
  project               = var.project_id
  region                = var.secondary_region
  network_endpoint_type = "SERVERLESS"
  cloud_run {
    service = var.secondary_service
  }
}

# Backend Service العالمي يوجّه للمنطقتين
resource "google_compute_backend_service" "default" {
  name                  = "${var.name}-backend-service"
  project               = var.project_id
  protocol              = "HTTPS"
  load_balancing_scheme = "EXTERNAL_MANAGED"

  # المنطقة الأساسية
  backend {
    group           = google_compute_region_network_endpoint_group.primary.id
    capacity_scaler = 1.0
  }

  # المنطقة الثانوية (Standby — تفعّل عند الحاجة أو بنسبة 0)
  backend {
    group           = google_compute_region_network_endpoint_group.secondary.id
    capacity_scaler = 0.0
  }
}

# URL Map
resource "google_compute_url_map" "default" {
  name            = "${var.name}-url-map"
  project         = var.project_id
  default_service = google_compute_backend_service.default.id
}

# HTTPS Proxy
resource "google_compute_target_https_proxy" "default" {
  name             = "${var.name}-https-proxy"
  project          = var.project_id
  url_map          = google_compute_url_map.default.id
  ssl_certificates = [google_compute_managed_ssl_certificate.default.id]
}

# Global Forwarding Rule
resource "google_compute_global_forwarding_rule" "default" {
  name                  = "${var.name}-forwarding-rule"
  project               = var.project_id
  target                = google_compute_target_https_proxy.default.id
  port_range            = "443"
  ip_address            = google_compute_global_address.default.address
  load_balancing_scheme = "EXTERNAL_MANAGED"
}
