#variable "image" {}

variable "app_name" {
  default     = "robot-engine"
  description = "App name"
}

variable "app_port" {
  default     = "8086"
  description = "App port"
}

variable "region" {
  default     = "us-east-2"
  description = "The region the resources will be created in."
}

variable "image" {
  default     = "docker.io/hsouidi/robot-engine:20.6.11"
  description = "docker image"
}

variable "ecs_task_cpu" {
  description = "task CPU ."
}

variable "ecs_task_memory" {
  description = "task Memory ."
}

variable "ecs_desired_count" {
  description = "ECS desired count ."
}

variable "stage" {
  description = "Stage (dev,uat,production) ."
  default = "dev"
}

variable "profile" {
  description = "aws credentials profile ."
  default = "default"
}

variable "vpc_id" {
  description = "VPC id ."
}

variable "subnet_ids" {
  type = list(string)
  description = "List of subnet_ids"
}

