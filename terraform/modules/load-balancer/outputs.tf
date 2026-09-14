output "global_ip" {
  description = "عنوان IP العالمي لموزع الأحمال"
  value       = google_compute_global_address.default.address
}
