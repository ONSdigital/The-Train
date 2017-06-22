job "the-train" {
  datacenters = ["eu-west-1"]
  region      = "eu"
  type        = "service"

  update {
    stagger      = "90s"
    max_parallel = 1
  }

  group "web" {
    count = 2

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
          "-Xmx4094m",
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
        cpu    = 1500
        memory = 4096

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
