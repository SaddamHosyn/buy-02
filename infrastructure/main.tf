terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC and Security Groups
resource "aws_security_group" "jenkins_sg" {
  name        = "buy01-jenkins-sg"
  description = "Security group for Jenkins server"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Jenkins Web UI"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "buy01-jenkins-sg"
  }
}

resource "aws_security_group" "deployment_sg" {
  name        = "buy01-deployment-sg"
  description = "Security group for application deployment server"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Frontend"
    from_port   = 4200
    to_port     = 4200
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "API Gateway"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Eureka"
    from_port   = 8761
    to_port     = 8761
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "User Service"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Product Service"
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Media Service"
    from_port   = 8083
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "buy01-deployment-sg"
  }
}

# Elastic IPs (static IPs)
resource "aws_eip" "jenkins_eip" {
  instance = aws_instance.jenkins.id
  domain   = "vpc"

  tags = {
    Name = "buy01-jenkins-eip"
  }
}

resource "aws_eip" "deployment_eip" {
  instance = aws_instance.deployment.id
  domain   = "vpc"

  tags = {
    Name = "buy01-deployment-eip"
  }
}

# Jenkins Instance
resource "aws_instance" "jenkins" {
  ami           = var.amazon_linux_ami
  instance_type = "m7i-flex.large"
  key_name      = var.key_pair_name

  vpc_security_group_ids = [aws_security_group.jenkins_sg.id]

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = file("${path.module}/scripts/setup-jenkins.sh")

  tags = {
    Name = "buy01-jenkins-server"
  }
}

# Deployment Instance
resource "aws_instance" "deployment" {
  ami           = var.amazon_linux_ami
  instance_type = "m7i-flex.large"
  key_name      = var.key_pair_name

  vpc_security_group_ids = [aws_security_group.deployment_sg.id]

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = file("${path.module}/scripts/setup-deployment.sh")

  tags = {
    Name = "buy01-deployment-server"
  }
}
