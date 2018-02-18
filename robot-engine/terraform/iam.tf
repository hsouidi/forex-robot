/*
 * Create ECS IAM Instance Role and Policy
 * Use random id in naming of roles to prevent collisions
 * should other ECS clusters be created in same AWS account
 * using this same code.
 */
resource "random_id" "code" {
  byte_length = 4
}

/*
 * Create ECS IAM Task Role and Policy
 */
resource "aws_iam_role" "ecsTaskRole" {
  name = "ecsTaskRole-${random_id.code.hex}"

  assume_role_policy = <<EOF
{
 "Version": "2008-10-17",
 "Statement": [
   {
     "Sid": "",
     "Effect": "Allow",
     "Principal": {
       "Service": "ecs-tasks.amazonaws.com"
     },
     "Action": "sts:AssumeRole"
   }
 ]
}
EOF
}

resource "aws_iam_role_policy" "ecsTaskRolePolicy" {
  name = "ecsTaskRolePolicy-${random_id.code.hex}"
  role = aws_iam_role.ecsTaskRole.id

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:Encrypt",
        "s3:ListBucket",
        "s3:*Object",
        "secretsmanager:GetSecretValue",
        "tag:GetResources",
        "dynamodb:List*",
        "dynamodb:BatchGet*",
        "dynamodb:DescribeTable",
        "dynamodb:Get*",
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:BatchWrite*",
        "dynamodb:CreateTable",
        "dynamodb:Delete*",
        "dynamodb:Update*",
        "dynamodb:PutItem"
      ],
      "Resource": [
        "*"
      ]
    }
  ]
}
EOF
}

# https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_execution_IAM_role.html
resource "aws_iam_role" "ecsTaskExecutionRole" {
  name = "ecsTaskExecutionRole-${random_id.code.hex}"
  assume_role_policy = data.aws_iam_policy_document.assume_role_policy.json
}

# allow task execution role to be assumed by ecs
data "aws_iam_policy_document" "assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}


# Define policy ARNs as list
variable "iam_policy_arn" {
  description = "IAM Policy to be attached to role"
  type = list(string)
  default = ["arn:aws:iam::aws:policy/CloudWatchLogsFullAccess","arn:aws:iam::aws:policy/CloudWatchEventsFullAccess","arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"]
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionRolePolicy-attach" {
role       = aws_iam_role.ecsTaskExecutionRole.name
count      = length(var.iam_policy_arn)
policy_arn = var.iam_policy_arn[count.index]
}
