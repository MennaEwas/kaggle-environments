# Just build the orchestrator container
path=$(dirname $0)
# cd to the parent directory to include kaggle_environments folder in Docker build context
cd $path/..
docker build -f ./docker/Dockerfile -t python-simulations .
cd -