import React from 'react';
import { AppBar, Box, Button, Card, CardContent, Container, Grid, IconButton, Toolbar, Typography } from '@mui/material';
import { GitHub as GitHubIcon } from '@mui/icons-material';
import { Link as ScrollLink } from 'react-scroll';

const sections = [
  { id: 'home', label: '首页' },
  { id: 'features', label: '功能特点' },
  { id: 'usage', label: '使用场景' },
];

const features = [
  { title: '自动重复下载', description: '支持理论无上限次数的海量图片下载，尤其在捕获对于非常多数据的图片API库时表现出色。' },
  { title: '错误处理与恢复', description: '具备完善的错误处理机制，能够在网络中断或其他异常情况下自动重试，并从上次中断的位置继续下载，避免重复工作。' },
  { title: '兼容性广泛', description: '采用Java编写，具有良好的跨平台特性，能够在多种操作系统上运行，如Windows、Linux和macOS。' },
  { title: '可视化窗口', description: '编写了图形化窗口，便于快捷使用，告别终端操作。' },
];

const usageScenarios = [
  '从图片托管服务的API获取用户上传的图片资源，用于本地备份或进一步处理。',
  '在数据科学领域，从公开的数据集API下载图片数据，为机器学习模型训练提供素材。',
  '为网络爬虫应用提供图片下载功能，配合爬虫程序抓取网页中的图片内容。',
  '需要获取网络图片作为壁纸等。',
];

function App() {
  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="fixed">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Imget
          </Typography>
          <Box sx={{ display: { xs: 'none', md: 'flex' } }}>
            {sections.map((section) => (
              <Button
                key={section.id}
                color="inherit"
                component={ScrollLink}
                to={section.id}
                spy={true}
                smooth={true}
                offset={-64}
              >
                {section.label}
              </Button>
            ))}
          </Box>
          <IconButton
            color="inherit"
            href="https://github.com/your-username/Imget"
            target="_blank"
            rel="noopener noreferrer"
          >
            <GitHubIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Box id="home" sx={{ pt: 8, minHeight: '100vh', display: 'flex', alignItems: 'center' }}>
        <Container>
          <Grid container spacing={4} alignItems="center">
            <Grid item xs={12} md={6}>
              <Typography variant="h2" component="h1" gutterBottom>
                Imget
              </Typography>
              <Typography variant="h5" color="text.secondary" paragraph>
                一款专为高效下载图片API而设计的应用程序
              </Typography>
              <Typography variant="body1" paragraph>
                目前，我们能遇到许多随机加载的图片API站点，它们均有一个庞大的图片库，但是我们要下载这里面的图片那下载步骤就非常繁琐。Imget让这个过程变得简单高效。
              </Typography>
              <Button variant="contained" size="large" href="#download">
                立即下载
              </Button>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box component="img" src="/Imget.png" alt="Imget Screenshot" sx={{ width: '100%', borderRadius: 2 }} />
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Box id="features" sx={{ py: 8, bgcolor: 'background.paper' }}>
        <Container>
          <Typography variant="h3" component="h2" textAlign="center" gutterBottom>
            功能特点
          </Typography>
          <Grid container spacing={4}>
            {features.map((feature, index) => (
              <Grid item xs={12} md={6} key={index}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography variant="h5" component="h3" gutterBottom>
                      {feature.title}
                    </Typography>
                    <Typography variant="body1">{feature.description}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      <Box id="usage" sx={{ py: 8 }}>
        <Container>
          <Typography variant="h3" component="h2" textAlign="center" gutterBottom>
            使用场景
          </Typography>
          <Grid container spacing={4}>
            {usageScenarios.map((scenario, index) => (
              <Grid item xs={12} md={6} key={index}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography variant="body1">{scenario}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>
    </Box>
  );
}

export default App;