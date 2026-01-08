variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "eu-north-1"
}

variable "amazon_linux_ami" {
  description = "Amazon Linux 2023 AMI ID"
  type        = string
  # eu-north-1 Amazon Linux 2023 AMI
  default = "ami-0683ee28af6610487"
}

variable "key_pair_name" {
  description = "Name of the SSH key pair to use for instances"
  type        = string
  default     = "lastreal"
}
