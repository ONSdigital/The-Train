job "the-train" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  update {
    min_healthy_time = "30s"
    healthy_deadline = "2m"
    max_parallel     = 1
    auto_revert      = true
    stagger          = "150s"
  }

  group "web" {
    count = "{{WEB_TASK_COUNT}}"

    spread {
      attribute = "${node.unique.id}"
      weight    = 100
      # with `target` omitted, Nomad will spread allocations evenly across all values of the attribute.
    }
    spread {
      attribute = "${attr.platform.aws.placement.availability-zone}"
      weight    = 100
      # with `target` omitted, Nomad will spread allocations evenly across all values of the attribute.
    }

    constraint {
      attribute = "${node.class}"
      value     = "web-mount"
    }

    restart {
      attempts = 3
      delay    = "15s"
      interval = "1m"
      mode     = "delay"
    }

    task "the-train" {
      driver = "docker"

      artifact {
        source = "s3::https://s3-eu-west-1.amazonaws.com/{{DEPLOYMENT_BUCKET}}/the-train/{{REVISION}}.tar.gz"
      }

      config {
        command     = "${NOMAD_TASK_DIR}/start-task"
        image       = "{{ECR_URL}}:concourse-{{REVISION}}"
        userns_mode = "host"

        args = [
          "java",
          "-server",
          "-Xms{{WEB_RESOURCE_HEAP_MEM}}m",
          "-Xmx{{WEB_RESOURCE_HEAP_MEM}}m",
          "-Drestolino.files=target/web",
          "-jar target/*-jar-with-dependencies.jar",
        ]

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
