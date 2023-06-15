# Tekton

To enable access to Docker in Tekton, a dind sidecar needs to be added. An example of it can be found 
[here](https://github.com/tektoncd/pipeline/blob/main/examples/v1beta1/taskruns/dind-sidecar.yaml)

This is an example

```yaml
apiVersion: tekton.dev/v1beta1
kind: Task
metadata:
  name: run-tests
  description: Run Tests
spec:
  workspaces:
    - name: source
  steps:
    - name: read
      image: eclipse-temurin:17.0.3_7-jdk-alpine
      workingDir: $(workspaces.source.path)
      script: ./mvnw test
      volumeMounts:
        - mountPath: /var/run/
          name: dind-socket
  sidecars:
    - image: docker:20.10-dind
      name: docker
      securityContext:
        privileged: true
      volumeMounts:
        - mountPath: /var/lib/docker
          name: dind-storage
        - mountPath: /var/run/
          name: dind-socket
  volumes:
    - name: dind-storage
      emptyDir: { }
    - name: dind-socket
      emptyDir: { }
---
apiVersion: tekton.dev/v1beta1
kind: Pipeline
metadata:
  name: testcontainers-demo
spec:
  description: |
    This pipeline clones a git repo, run testcontainers.
  params:
    - name: repo-url
      type: string
      description: The git repo URL to clone from.
  workspaces:
    - name: shared-data
      description: |
        This workspace contains the cloned repo files, so they can be read by the
        next task.
  tasks:
    - name: fetch-source
      taskRef:
        name: git-clone
      workspaces:
        - name: output
          workspace: shared-data
      params:
        - name: url
          value: $(params.repo-url)
    - name: run-tests
      runAfter: ["fetch-source"]
      taskRef:
        name: run-tests
      workspaces:
        - name: source
          workspace: shared-data
---
apiVersion: tekton.dev/v1beta1
kind: PipelineRun
metadata:
  name: testcontainers-demo-run
spec:
  pipelineRef:
    name: testcontainers-demo
  workspaces:
    - name: shared-data
      volumeClaimTemplate:
        spec:
          accessModes:
            - ReadWriteOnce
          resources:
            requests:
              storage: 1Gi
  params:
    - name: repo-url
      value: https://github.com/testcontainers/testcontainers-java-repro.git
```
