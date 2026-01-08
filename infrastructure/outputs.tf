output "jenkins_public_ip" {
  description = "Jenkins server public IP (static)"
  value       = aws_eip.jenkins_eip.public_ip
}

output "deployment_public_ip" {
  description = "Deployment server public IP (static)"
  value       = aws_eip.deployment_eip.public_ip
}

output "jenkins_url" {
  description = "Jenkins Web UI URL"
  value       = "http://${aws_eip.jenkins_eip.public_ip}:8080"
}

output "frontend_url" {
  description = "Application Frontend URL"
  value       = "http://${aws_eip.deployment_eip.public_ip}:4200"
}

output "api_gateway_url" {
  description = "API Gateway URL"
  value       = "http://${aws_eip.deployment_eip.public_ip}:8080"
}

output "eureka_url" {
  description = "Eureka Dashboard URL"
  value       = "http://${aws_eip.deployment_eip.public_ip}:8761"
}

output "next_steps" {
  description = "Next steps after infrastructure creation"
  value       = <<-EOT
  
  ==========================================
  Infrastructure Created Successfully!
  ==========================================
  
  Jenkins Server: ${aws_eip.jenkins_eip.public_ip}
  Deployment Server: ${aws_eip.deployment_eip.public_ip}
  
  Next Steps:
  1. Wait 5-10 minutes for instances to complete setup
  2. Run migration script:
     ./infrastructure/migrate-to-new-servers.sh ${aws_eip.jenkins_eip.public_ip} ${aws_eip.deployment_eip.public_ip}
  
  3. Access Jenkins: http://${aws_eip.jenkins_eip.public_ip}:8080
  4. Access Frontend: http://${aws_eip.deployment_eip.public_ip}:4200
  
  EOT
}
