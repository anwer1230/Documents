output "service_name" {
  value = google_cloud_run_service.app.name
}

output "service_url" {
  value = google_cloud_run_service.app.status[0].url
}

output "location" {
  value = google_cloud_run_service.app.location
}
