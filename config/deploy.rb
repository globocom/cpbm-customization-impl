require 'capistrano'

desc "dev Environment"
task :dev do
  puts "Deploying environment dev"
  set :stage, "dev"
  set :user, 'cpbm'
  role :server, "cittamp13lc05vldc03.globoi.com", "cittamp13lc05vldc031.globoi.com"
end

desc "qa1 Environment"
task :qa1 do
  puts "Deploying environment qa1"
  set :stage, "qa1"
  set :user, 'cpbm'
  role :server, "cittamp13lc05vldc02.globoi.com", "cittamp13lc05vldc021.globoi.com", "cittamp13lc05vldc022.globoi.com"
end

desc "prod Environment"
task :prod do
  puts "Deploying environment prod"
  set :stage, "prod"
  set :user, 'cpbm'
  role :server, "rjebtcpbm01.globoi.com" ,"rjebtcpbm02.globoi.com", "rjebtcpbm03.globoi.com"
  #role :server, "rjebtcpbm02.globoi.com"
end

before("deploy:all", "deploy:clean")
# after("deploy:all", "virgo:restart")
# after("deploy:model", "virgo:restart")
# after("deploy:common", "virgo:restart")
# after("deploy:portal", "virgo:restart")

namespace :deploy do
  
  desc "Deploy update code"
  task :update_code, :roles => :server do
    deploy.all
  end
  
  desc "Deploy create_symlink"
  task :create_symlink, :roles => :server do
    puts "nothing to do..."
  end
  
  desc "Custom model deploy"
  task :model, :roles => :server do
     puts "Deploying citrix.cpbm.custom.model"
     set :deploy_dir, "/usr/share/vts3/custom_impls"
     set :project_dir, "src/citrix.cpbm.custom.model" # directory where is pom.xml
     set :artifactId, "citrix.cpbm.custom.model" # see pom.xml
     maven.compile
     top.upload("#{get_file.chomp}", "#{deploy_dir}", :via => :scp)
  end

  desc "Custom common deploy"
  task :common, :roles => :server do
     puts "Deploying citrix.cpbm.custom.common"
     set :deploy_dir, "/usr/share/vts3/custom_impls"
     set :project_dir, "src/citrix.cpbm.custom.common" # directory where is pom.xml
     set :artifactId, "citrix.cpbm.custom.common" # see pom.xml
     maven.compile
     top.upload("#{get_file.chomp}", "#{deploy_dir}", :via => :scp)
  end

  desc "Custom portal deploy"
  task :portal, :roles => :server do
     puts "Deploying citrix.cpbm.custom.portal"
     set :deploy_dir, "/usr/share/vts3/custom_impls"
     set :project_dir, "src/citrix.cpbm.custom.portal" # directory where is pom.xml
     set :artifactId, "citrix.cpbm.custom.portal" # see pom.xml
     maven.compile
     top.upload("#{get_file.chomp}", "#{deploy_dir}", :via => :scp)
  end

  desc "Custom deploy all"
  task :all, :roles => :server do
     puts "Deploying citrix.cpbm.custom.all ( model, common, portal )"
     set :deploy_dir, "/usr/share/vts3/custom_impls"
     set :project_dir, "src/citrix.cpbm.custom.all" # directory where is pom.xml
     set :artifactId, "citrix.cpbm.custom"
     maven.compile
     system("/bin/mkdir -p #{project_dir}/target")
     system("/bin/cp src/citrix.cpbm.custom.model/target/citrix.cpbm.custom.model*.jar #{project_dir}/target")
     system("/bin/cp src/citrix.cpbm.custom.common/target/citrix.cpbm.custom.common*.jar #{project_dir}/target")
     system("/bin/cp src/citrix.cpbm.custom.portal/target/citrix.cpbm.custom.portal*.jar #{project_dir}/target")
     files = get_file.split("\n")
     files.each do |i|
        puts "uploading file #{i}"
        top.upload("#{i}", "#{deploy_dir}", :via => :scp)
     end
  end

  desc "Clean all"
  task :clean, :roles => :server do
     puts "Clean citrix.cpbm.custom.all ( model, common, portal )"
     set :deploy_dir, "/usr/share/vts3/custom_impls"
     run("if /bin/ls  #{deploy_dir}/citrix.cpbm.custom*.jar > /dev/null 2>&1; then /bin/rm #{deploy_dir}/citrix.cpbm.custom*.jar;fi")
  end

end

namespace :maven do
  task :compile do
     desc "running mvn clean install"
     system("cd #{project_dir}; mvn clean install -Dmaven.test.skip=true")
  end
end

namespace :virgo do
  desc "service virgo restart"
  task :restart do
     puts "restart virgo"
     stop
     start
  end
  
  desc "service virgo start"
  task :start do
     puts "starting cpbm"
     run("sudo /etc/init.d/cloud-portal start")
  end
  
  desc "service virgo stop"
  task :stop do
    puts "stopping cpbm"
     run("sudo /etc/init.d/cloud-portal stop")
  end
end


def get_file()
  file = `/bin/ls #{project_dir}/target/#{artifactId}*.jar`
  return file
end

