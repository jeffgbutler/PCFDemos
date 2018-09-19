using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using NetLoanCalculator.Services;
using Steeltoe.CloudFoundry.Connector.Redis;
using Steeltoe.Extensions.Configuration.CloudFoundry;

namespace NetLoanCalculatorRedis
{
    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddCors();
            services.AddMvc().SetCompatibilityVersion(CompatibilityVersion.Version_2_1);

            services.AddOptions();
            services.ConfigureCloudFoundryOptions(Configuration);

            var instanceId = Configuration["vcap:application:instance_id"];
            if (instanceId == null)
            {
                services.AddSingleton<IHitCountService, MemoryHitCountService>();
            }
            else
            {
                services.AddSingleton<IHitCountService, RedisHitCountService>();
            }


            services.AddSingleton<PaymentCalculator>(new PaymentCalculator());
            services.AddSingleton<Crasher>(new Crasher());

            services.AddRedisConnectionMultiplexer(Configuration);
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }
            else
            {
                app.UseHsts();
            }

            app.UseCors(builder => builder.AllowAnyOrigin());
            app.UseHttpsRedirection();
            app.UseMvc();
        }
    }
}
