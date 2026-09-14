terraform {
  backend "gcs" {
    bucket = "documents-staging-tfstate"
    prefix = "terraform/state"
  }
}
