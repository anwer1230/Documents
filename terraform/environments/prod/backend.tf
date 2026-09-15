terraform {
  backend "gcs" {
    bucket = "documents-prod-tfstate"
    prefix = "terraform/state"
  }
}
