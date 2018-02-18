
resource "aws_security_group" "allow_webapp_port_sg" {
  name        = "allow_webapp_port_sg"
  description = "Allow robot webapp_port_sg"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

/*
 * Create ECS cluster
 */
resource "aws_ecs_cluster" "ecs_cluster" {
  name = "${var.app_name}-cluster-${var.stage}"
}

resource "aws_cloudwatch_log_group" "robot_log_group" {

  name = "/ecs/${var.app_name}-${var.stage}"

}

/*
 * Create ECS Service
 */
resource "aws_ecs_service" "service" {
  name                               = "${var.app_name}-${var.stage}"
  cluster                            = aws_ecs_cluster.ecs_cluster.name
  desired_count                      = 1
  deployment_maximum_percent         = "200"
  deployment_minimum_healthy_percent = "100"
  launch_type ="FARGATE"

  network_configuration {
    security_groups = [aws_security_group.allow_webapp_port_sg.id]
    subnets = var.subnet_ids
    assign_public_ip = true
  }

  task_definition = "${aws_ecs_task_definition.app.family}:${aws_ecs_task_definition.app.revision}"
}


resource "aws_ecs_task_definition" "app" {
  family                   = "${var.app_name}-task-${var.stage}"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.ecs_task_cpu
  memory                   = var.ecs_task_memory
  execution_role_arn       = aws_iam_role.ecsTaskExecutionRole.arn
  network_mode             = "awsvpc"

  # defined in role.tf
  task_role_arn = aws_iam_role.ecsTaskRole.arn

  container_definitions = <<DEFINITION
 [
    {
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "${var.stage}"
        }
      ],
      "name": "${var.app_name}-container-${var.stage}",
      "mountPoints": [],
      "image": "${var.image}",
      "cpu": ${var.ecs_task_cpu},
      "memory": ${var.ecs_task_memory},
      "portMappings": [
        {
          "protocol": "tcp",
          "containerPort": ${var.app_port},
          "hostPort": ${var.app_port}
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-region": "${var.region}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-group": "${aws_cloudwatch_log_group.robot_log_group.name}"
        }
      },
      "essential": true,
      "volumesFrom": []
    }
  ]
DEFINITION
}