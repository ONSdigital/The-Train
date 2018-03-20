job "the-train" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  constraint {
    attribute = "${meta.has_disk}"
    value     = true
  }

  update {
    stagger      = "90s"
    max_parallel = 1
  }

  group "web" {
    count = "{{WEB_TASK_COUNT}}"

    constraint {
      distinct_hosts = true
    }

    constraint {
      attribute = "${node.class}"
      value     = "web"
    }

    task "the-train" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/the-train/{{REVISION}}.tar.gz"
      }

      config {
        command = "${NOMAD_TASK_DIR}/start-task"

        args = [
          "java",
          "-server",
          "-Xms{{PUBLISHING_RESOURCE_HEAP_MEM}}m",
          "-Xmx{{WEB_RESOURCE_HEAP_MEM}}m",
          "-Drestolino.files=target/web",
          "-jar target/*-jar-with-dependencies.jar",
        ]

        image = "{{ECR_URL}}:concourse-{{REVISION}}"

        port_map {
          http = 8080
        }

        volumes = [
          "/var/babbage/tmp:/tmp",
          "/var/babbage/site:/content",
          "/var/babbage/publishing:/transactions",
        ]
      }

      service {
        name = "the-train"
        port = "http"
        tags = ["web"]
      }

      resources {
        cpu    = "{{WEB_RESOURCE_CPU}}"
        memory = "{{WEB_RESOURCE_MEM}}"

        network {
          port "http" {}
        }
      }

      template {
        source      = "${NOMAD_TASK_DIR}/vars-template"
        destination = "${NOMAD_TASK_DIR}/vars"
      }

      vault {
        policies = ["the-train"]
      }
    }
  }
}
