provider "aws" {
  #region     = var.aws_region
  region  = var.region
  profile = var.profile
}
terraform {
  required_version = ">= 0.12"
  backend "s3" {}
}

