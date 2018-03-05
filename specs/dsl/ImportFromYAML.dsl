def projName = args.projectName
def params = args.params

project projName, {
  procedure 'ImportFromYAML', {
    params.each {k, v ->
      formalParameter k, defaultValue: '', {
        type = 'textarea'
      }
    }
    step 'ImportFromYAML', {
      subproject = '/plugins/EC-Kubernetes/project'
      subprocedure = 'ImportFromYAML'

      params.each { k, v ->
        actualParameter k, value: '$[' + k + ']'
      }
    }
  }
}
